package com.stefan.simplebackup.utils.file

import java.io.File

data class FileWatchEvent(
    val file: File,
    val state: State
) {
    enum class State(val state: String) {
        Modified("modified"),
        Created("created"),
        Deleted("deleted")
    }
}
