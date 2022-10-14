package com.stefan.simplebackup.utils.file

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey

// File extension function. Get the RecursiveFileWatcher instance from File.
fun File.asRecursiveFileWatcher(scope: CoroutineScope) =
    RecursiveFileWatcher(scope = scope, rootDir = this)

@Suppress("BlockingMethodInNonBlockingContext")
// Suppress the warning, we are running this code on IO Dispatcher
class RecursiveFileWatcher(private val scope: CoroutineScope, private val rootDir: File) {

    private val ioDispatcher = Dispatchers.IO
    private val registeredKeys = HashMap<WatchKey, Path>()
    private val watchService = FileSystems.getDefault().newWatchService()

    private val _fileEvent = MutableSharedFlow<FileEvent>()
    val fileEvent get() = _fileEvent.asSharedFlow().distinctUntilChanged()

    init {
        processFileEvents()
    }

    private fun processFileEvents() = scope.launch(ioDispatcher) {
        rootDir.mkdirs()
        var shouldRegisterNewPaths = true
        // Emit an event while the coroutine job is still active
        while (isActive) {
            run polling@{
                if (shouldRegisterNewPaths) {
                    registerDirsRecursively()
                    shouldRegisterNewPaths = false
                }

                val newMonitorKey = try {
                    watchService.take()
                } catch (e: InterruptedException) {
                    Log.e("RecursiveFileWatcher", "$e")
                    return@polling
                }

                val filePath = registeredKeys[newMonitorKey] ?: return@polling

                newMonitorKey.pollEvents().forEach { watchEvent ->
                    val watchEventKind = watchEvent.kind()

                    // Handle OVERFLOW event, by skipping it.
                    if (watchEventKind == OVERFLOW) {
                        Log.w("RecursiveFileWatcher", "An overflow has occurred!")
                        return@forEach
                    }

                    val eventKind = when (watchEventKind) {
                        ENTRY_CREATE -> EventKind.CREATED
                        ENTRY_DELETE -> EventKind.DELETED
                        else -> EventKind.MODIFIED
                    }
                    val eventFile = filePath.resolve(watchEvent.context() as Path).toFile()

                    if (eventKind == EventKind.MODIFIED)
                        acquireLockOnModifiedEvent(modifiedFile = eventFile)

                    // If any directory is created or deleted, re-register the whole dir tree.
                    if (eventKind != EventKind.MODIFIED && eventFile.isDirectory)
                        shouldRegisterNewPaths = true

                    // Finally emit the event
                    val event = FileEvent(file = eventFile, kind = eventKind)
                    _fileEvent.emit(event).also {
                        Log.d("RecursiveFileWatcher", "Emitting $event")
                    }
                }

                // Reset key and remove it from HashMap if directory no longer accessible
                val isNewMonitorKeyValid = newMonitorKey.reset()
                if (!isNewMonitorKeyValid) {
                    if (registeredKeys[newMonitorKey] == rootDir.toPath()) {
                        rootDir.mkdirs()
                        shouldRegisterNewPaths = true
                    }
                    registeredKeys.remove(newMonitorKey)
                }
            }
        }
    }

    private fun acquireLockOnModifiedEvent(modifiedFile: File) {
        val fileChannel = FileInputStream(modifiedFile).channel
        while (true) {
            try {
                Log.d("RecursiveFileWatcher", "Acquiring lock for ${modifiedFile.absolutePath}")
                if (fileChannel.tryLock(0L, Long.MAX_VALUE, true) != null) break
            } catch (e: IOException) {
                Log.e("RecursiveFileWatcher", "Exception while acquiring $e")
            } finally {
                fileChannel.close()
            }
        }
    }

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
            Log.e("RecursiveFileWatcher", "I/O Error, unable to register new WatchKey $e")
        }
    }

    // WatchEvent wrapper
    data class FileEvent(val file: File, val kind: EventKind)
}

// WatchEvent.Kind wrapper
enum class EventKind {
    CREATED, DELETED, MODIFIED
}