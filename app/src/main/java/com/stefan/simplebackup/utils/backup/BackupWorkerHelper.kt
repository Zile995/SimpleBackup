package com.stefan.simplebackup.utils.backup

import androidx.work.*
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.workers.BackupWorker
import java.util.*

const val REQUEST_TAG = "BACKUP_TAG"
const val BACKUP_ARGUMENT = "BACKUP_PACKAGES"
const val BACKUP_WORK_NAME = "BACKUP_WORK"

class BackupWorkerHelper(
    private val appList: MutableList<AppData>,
    private val workManager: WorkManager
) {

    constructor(app: AppData, workManager: WorkManager) : this(
        mutableListOf(app),
        workManager
    )

    fun startBackupWorker() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(createInputData())
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(REQUEST_TAG)
            .build()

        workManager.beginUniqueWork(BACKUP_WORK_NAME, ExistingWorkPolicy.REPLACE, backupRequest)
            .enqueue()
    }

    private fun createInputData(): Data {
        val builder = Data.Builder()
        val packageNames = arrayListOf<String>()
        appList.forEach { app ->
            packageNames.add(app.getPackageName())
        }
        builder.putStringArray(BACKUP_ARGUMENT, packageNames.toTypedArray())
        return builder.build()
    }
}