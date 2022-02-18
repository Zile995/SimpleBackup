package com.stefan.simplebackup.utils.backup

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.AppData
import java.text.SimpleDateFormat
import java.util.*

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(
    context: Context,
    private val appList: MutableList<AppData>
) : BackupHelper(context) {

    private val zipUtil = ZipUtil(context, appList)
    private val tarUtil = TarUtil(context, appList)

    suspend fun backup() {
        Log.d("BackupUtil", "Got ${appList.map { it.getName() }} apps")
        createMainDir()
        createAppBackupDirs()
        tarUtil.backupData()
        zipUtil.zipAllData()
    }

    private suspend fun createAppBackupDirs() {
        appList.forEach {
            createAppBackupDir(getBackupDirPath(it))
        }
    }
}