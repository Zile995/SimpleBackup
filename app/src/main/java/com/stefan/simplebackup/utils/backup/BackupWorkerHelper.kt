package com.stefan.simplebackup.utils.backup

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.workers.BackupWorker

class BackupWorkerHelper(private val appList: MutableList<AppData>, application: DatabaseApplication) {

    constructor(app: AppData, application: DatabaseApplication) : this(
        mutableListOf(app),
        application
    )

    private val workManager = WorkManager.getInstance(application)

    fun startBackupWorker() {
        val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(createInputData())
            .build()
        workManager.enqueue(backupRequest)
    }

    private fun createInputData(): Data {
        val builder = Data.Builder()
        val backupDirPaths = arrayListOf<String>()
        appList.forEach { app ->
            backupDirPaths.add(app.getPackageName())
        }
        builder.putStringArray("BACKUP_PACKAGES", backupDirPaths.toTypedArray())
        return builder.build()
    }
}