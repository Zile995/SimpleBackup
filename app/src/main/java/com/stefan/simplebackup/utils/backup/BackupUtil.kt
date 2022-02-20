package com.stefan.simplebackup.utils.backup

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.AppData
import java.text.SimpleDateFormat
import java.util.*

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(
    appContext: Context,
    private val appList: MutableList<AppData>
) : BackupHelper(appContext) {

    private val context = appContext

    suspend fun backup() {
        Log.d("BackupUtil", "Got ${appList.map { it.getName() }} apps")
        createMainDir()
        appList.forEach { backupApp ->
            val tarUtil = TarUtil(context, backupApp)
            val zipUtil = ZipUtil(context, backupApp)
            createAppBackupDir(getBackupDirPath(backupApp))
            tarUtil.backupData()
            zipUtil.zipAllData()
            setBackupTime(backupApp)
            serializeApp(backupApp)
        }
    }
}