package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.utils.extensions.ioDispatcher
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

fun File.asRecursiveFileWatcher() = RecursiveFileWatcher(toPath())

class RecursiveFileWatcher(private val path: Path) {

    private val registeredKeys = HashMap<WatchKey, Path>()
    private val watchService = FileSystems.getDefault().newWatchService()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun processFileEvents() = flow {
        var shouldRegisterNewPaths = true
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

                if (eventKind != EventKind.MODIFIED && eventFile.isDirectory)
                    shouldRegisterNewPaths = true

                val event = FileEvent(file = eventFile, kind = eventKind)
                emit(event).also { Log.d("FileWatcher", "Emitting $event") }
            }

            val isNewMonitorKeyValid = newMonitorKey.reset()
            if (!isNewMonitorKeyValid) {
                registeredKeys.remove(newMonitorKey)
            }
        }
    }.flowOn(ioDispatcher)

    private fun registerDirsRecursively() {
        // register directory and sub-directories
        if (registeredKeys.isNotEmpty()) {
            registeredKeys.apply {
                forEach { it.key.cancel() }
                clear()
            }
        }
        path.toFile().walkTopDown().filter { file ->
            file.isDirectory
        }.forEach { directory ->
            Log.d("FileWatcher", "Registering ${directory.absolutePath}")
            registerDir(directory.toPath())
        }
    }

    private fun registerDir(filePath: Path) {
        try {
            val newWatchKey =
                filePath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            registeredKeys[newWatchKey] = filePath
        } catch (e: IOException) {
            Log.e("FileWatcher", "I/O Error, unable to register new watch key")
        }
    }

    data class FileEvent(
        val file: File,
        val kind: EventKind
    )
}

enum class EventKind(val value: String) {
    CREATED("created"),
    DELETED("deleted"),
    MODIFIED("modified")
}