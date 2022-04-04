package com.stefan.simplebackup.utils.backup

import androidx.work.*
import com.stefan.simplebackup.workers.BackupWorker

const val BACKUP_REQUEST_TAG = "BACKUP_TAG"
const val BACKUP_ARGUMENT = "BACKUP_PACKAGES"
const val BACKUP_WORK_NAME = "BACKUP_WORK"
const val BACKUP_SIZE = "NUMBER_OF_BACKED_UP"

class BackupWorkerHelper(
    private val appList: Array<String>,
    private val workManager: WorkManager
) {

    constructor(packageName: String, workManager: WorkManager) : this(
        arrayOf(packageName),
        workManager
    )

    fun startBackupWorker() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(createInputData())
            .setConstraints(constraints)
            .addTag(BACKUP_REQUEST_TAG)
            .build()

        workManager.beginUniqueWork(BACKUP_WORK_NAME, ExistingWorkPolicy.REPLACE, backupRequest)
            .enqueue()
    }

    private fun createInputData(): Data {
        val builder = Data.Builder()
        builder.putStringArray(BACKUP_ARGUMENT, appList)
        return builder.build()
    }
}