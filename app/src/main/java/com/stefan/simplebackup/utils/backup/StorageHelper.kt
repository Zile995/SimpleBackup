package com.stefan.simplebackup.utils.backup

import android.content.Context
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import java.io.File

abstract class StorageHelper(context: Context) {

    private val internalStoragePath: String by lazy {
        context.getExternalFilesDir(null)?.absolutePath ?: ""
    }

    protected val mainBackupDirPath = internalStoragePath.let { path ->
        path.substring(0, path.indexOf("Android")) + ROOT
    }

    protected val privateDir: File = context.filesDir

    suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainBackupDirPath)
            createFile("$mainBackupDirPath/.nomedia")
        }
    }
}