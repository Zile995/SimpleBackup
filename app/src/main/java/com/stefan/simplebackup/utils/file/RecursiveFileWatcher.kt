package com.stefan.simplebackup.utils.file

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import kotlin.coroutines.coroutineContext

// File extension function. Get the RecursiveFileWatcher instance from File.
fun File.asRecursiveFileWatcher() = RecursiveFileWatcher(rootDir = this)

class RecursiveFileWatcher(private val rootDir: File) {

    private val ioDispatcher = Dispatchers.IO
    private val registeredKeys = HashMap<WatchKey, Path>()
    private val watchService = FileSystems.getDefault().newWatchService()

    init {
        if (!rootDir.isDirectory)
            throw IllegalArgumentException("rootDir must be verified to be directory beforehand.")
    }

    // Suppress the warning, we are running this code on IO Dispatcher
    @Suppress("BlockingMethodInNonBlockingContext")
    fun processFileEvents() = flow {
        var shouldRegisterNewPaths = true
        // Emit an event while the coroutine job is still active
        while (coroutineContext.isActive) {
            if (shouldRegisterNewPaths) {
                registerDirsRecursively()
                shouldRegisterNewPaths = false
            }

            val newMonitorKey = watchService.take()
            val filePath = registeredKeys[newMonitorKey] ?: break

            newMonitorKey.pollEvents().forEach { watchEvent ->
                val eventKind =
                    when (watchEvent.kind()) {
                        ENTRY_CREATE -> EventKind.CREATED
                        ENTRY_DELETE -> EventKind.DELETED
                        else -> EventKind.MODIFIED
                    }
                val eventFile = filePath.resolve(watchEvent.context() as Path).toFile()

                // If any directory is created or deleted, re-register the whole dir tree.
                if (eventKind != EventKind.MODIFIED && eventFile.isDirectory)
                    shouldRegisterNewPaths = true

                // Finally emit the event
                val event = FileEvent(file = eventFile, kind = eventKind)
                emit(event).also { Log.d("RecursiveFileWatcher", "Emitting $event") }
            }

            // Reset key and remove from HashMap if directory no longer accessible
            val isNewMonitorKeyValid = newMonitorKey.reset()
            if (!isNewMonitorKeyValid) {
                registeredKeys.remove(newMonitorKey)
            }
        }
    }.flowOn(ioDispatcher)

    private fun registerDirsRecursively() {
        // Cancel the key registration and clear the HashMap
        if (registeredKeys.isNotEmpty()) {
            registeredKeys.apply {
                forEach { it.key.cancel() }
                clear()
            }
        }
        // Register directory and sub-directories
        rootDir.walkTopDown().filter { file ->
            file.isDirectory
        }.forEach { directory ->
            Log.d("RecursiveFileWatcher", "Registering ${directory.absolutePath}")
            registerDir(directory.toPath())
        }
    }

    private fun registerDir(filePath: Path) {
        try {
            val newWatchKey =
                filePath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            registeredKeys[newWatchKey] = filePath
        } catch (e: IOException) {
            Log.e("RecursiveFileWatcher", "I/O Error, unable to register new watch key")
        }
    }

    // WatchEvent wrapper
    data class FileEvent(
        val file: File,
        val kind: EventKind
    )
}

// WatchEvent.Kind wrapper
enum class EventKind(val value: String) {
    CREATED("created"),
    DELETED("deleted"),
    MODIFIED("modified")
}