package com.stefan.simplebackup.utils

import java.io.File

class FileUtil private constructor() {

    companion object {
        fun createDirectory(path: String) {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        fun createFile(path: String) {
            val file = File(path)
            file.createNewFile()
        }
    }
}