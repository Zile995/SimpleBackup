package com.stefan.simplebackup.utils.backup

import android.icu.text.SimpleDateFormat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.FileUtil
import com.stefan.simplebackup.workers.BackupWorker
import java.util.*

class BackupHelper(private val appList: MutableList<AppData>, application: DatabaseApplication) {

    constructor(app: AppData, application: DatabaseApplication) : this(
        mutableListOf(app),
        application
    )

    private val mainDirPath by lazy { application.getMainBackupDirPath }
    private val backupDirPaths = arrayListOf<String>()
    private val workManager = WorkManager.getInstance(application)

    suspend fun localBackup() {
        prepare()
        val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(createInputData())
            .build()
        workManager.enqueue(backupRequest)
    }

    private fun createInputData(): Data {
        val builder = Data.Builder()
        builder.putStringArray("BACKUP_PATHS", backupDirPaths.toTypedArray())
        return builder.build()
    }

    private suspend fun prepare() {
        createMainDir()
        appList.forEach { app ->
            val backupDirPath = mainDirPath + "/" + app.getPackageName()
            addBackupDirPath(backupDirPath)
            createAppBackupDir(backupDirPath)
            setBackupTime(app)
            serializeApp(app, backupDirPath)
        }
    }

    private fun addBackupDirPath(backupDirPath: String) {
        backupDirPaths.add(backupDirPath)
    }

    private suspend fun serializeApp(app: AppData, backupDirPath: String) {
        FileUtil.serializeApp(app, backupDirPath)
    }

    private fun setBackupTime(app: AppData) {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app.setDate(time.format(Date()))
    }

    private suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainDirPath)
            createFile("${mainDirPath}/.nomedia")
        }
    }

    private suspend fun createAppBackupDir(backupDirPath: String) {
        FileUtil.createDirectory(backupDirPath)
    }

}