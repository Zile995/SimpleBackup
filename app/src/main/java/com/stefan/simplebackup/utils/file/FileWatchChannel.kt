package com.stefan.simplebackup.utils.file

import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

class FileWatchChannel(
    val file: File,
    val scope: CoroutineScope,
    val mode: Mode,
    private var channel: Channel<FileWatchEvent> = Channel()
) {
    init {
        scope.launch(ioDispatcher) {

        }
    }


}

enum class Mode {
    SingleFile,
    SingleDirectory,
    Recursive
}