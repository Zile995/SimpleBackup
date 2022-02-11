package com.stefan.simplebackup.utils.backup

import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil

abstract class StorageHelper(app: AppData?, internalStoragePath: String, ) {
    private val mainBackupDir = internalStoragePath.let { path ->
        path.substring(0, path.indexOf("Android")) + ROOT
    }

    protected val appBackupDir = mainBackupDir + "${app?.getName()}_${app?.getVersionName()}"

    suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainBackupDir)
            createFile("$mainBackupDir/.nomedia")
        }
    }
}