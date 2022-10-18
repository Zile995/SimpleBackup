package com.stefan.simplebackup.utils.file

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.*
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

    private var currentFileSize = 0L
    private val ioDispatcher = Dispatchers.IO
    private val registeredKeys = HashMap<WatchKey, Path>()
    private val watchService = FileSystems.getDefault().newWatchService()

    private val _fileEvent = MutableSharedFlow<FileEvent>(
        replay = 1,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val fileEvent get() = _fileEvent.asSharedFlow()

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

                    // If file is modified (the file is still being copied or written),
                    // wait before emitting MODIFIED event
                    if (eventKind == EventKind.MODIFIED && !eventFile.isDirectory) {
                        if (currentFileSize == eventFile.length())
                            return@forEach
                        else
                            acquireLockOnModifiedEvent(modifiedFile = eventFile)
                    } else currentFileSize = 0L

                    // If any directory is created or deleted, re-register the whole dir tree.
                    if (eventKind != EventKind.MODIFIED && eventFile.isDirectory)
                        shouldRegisterNewPaths = true

                    // Finally emit the event
                    val event = FileEvent(file = eventFile, kind = eventKind)
                    Log.d("RecursiveFileWatcher", "Emitting $event")
                    _fileEvent.tryEmit(event)
                }

                // Reset key and check if directory no longer accessible
                val isNewMonitorKeyValid = newMonitorKey.reset()
                if (!isNewMonitorKeyValid) {
                    if (registeredKeys[newMonitorKey] == rootDir.toPath()) {
                        // Re-create main rootDir and re-register dir tree
                        // Do not break watch service polling
                        rootDir.mkdirs()
                        shouldRegisterNewPaths = true
                    }
                    // Remove invalid key with an inaccessible path from HashMap
                    registeredKeys.remove(newMonitorKey)
                }
            }
        }
    }

    private fun acquireLockOnModifiedEvent(modifiedFile: File) {
        do {
            try {
                Log.d("RecursiveFileWatcher", "Acquiring lock for ${modifiedFile.absolutePath}")
                currentFileSize = 0L
                val fileInputStream = FileInputStream(modifiedFile)
                fileInputStream.use {
                    while (fileInputStream.available() > 0) {
                        // Read the 1024 bytes and calculate the current file size
                        val bytes = ByteArray(1024)
                        val numOfReadBytes = fileInputStream.read(bytes)
                        currentFileSize += numOfReadBytes.toLong()

                        // If end of the stream is reached, exit the inner while loop
                        if (numOfReadBytes < 0) break
                    }
                }
            } catch (e: Exception) {
                Log.w("RecursiveFileWatcher", "Exception while acquiring $e")
            }
        } while (currentFileSize != modifiedFile.length())
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