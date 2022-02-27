package com.stefan.simplebackup.utils.backup

import android.content.Context
import com.stefan.simplebackup.data.AppData

const val ROOT: String = "SimpleBackup/local"

open class BackupUtil(
    appContext: Context,
    private val app: AppData
) : BackupHelper(appContext) {

    private val zipUtil = ZipUtil(appContext, app)
    private val tarUtil = TarUtil(appContext, app)

    suspend fun backup() {
        createDirs()
        backupData()
        zipData()
        outputAppInfo()
    }

    private suspend fun createDirs() {
        createMainDir()
        createAppBackupDir(getBackupDirPath(app))
    }

    private suspend fun backupData() {
        tarUtil.backupData()
    }

    private suspend fun zipData() {
        zipUtil.zipAllData()
    }

    private suspend fun outputAppInfo() {
        setBackupTime(app)
        serializeApp(app)
    }
}