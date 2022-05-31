package com.stefan.simplebackup.utils.file

import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey

fun File.asFileWatcher() = FileWatcher(this.toPath())

@Suppress("BlockingMethodInNonBlockingContext")
class FileWatcher(private val path: Path) {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val keys = HashMap<WatchKey, Path>()
    // private var shouldRegisterPath = false

    init {
        path.registerAllDirs()
    }

    fun processEvents() = channelFlow<Nothing> {
        while (isActive) {




            delay(1_000L)
        }
    }.flowOn(ioDispatcher)

    @Throws(IOException::class)
    private fun Path.registerAllDirs() {
        // register directory and sub-directories
        toFile().walkTopDown().filter { file ->
            file.isFile && file.extension == "json"
        }.forEach { directory ->
            registerDir(directory.toPath())
        }
    }

    @Throws(IOException::class)
    private fun registerDir(newPath: Path) {
        val newKey = newPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keys[newKey] = newPath
    }

    data class FileEvent(
        val file: File,
        val kind: Kind
    )
}

enum class Kind(val value: String) {
    CREATED("created"),
    DELETED("deleted"),
    MODIFIED("modified")
}