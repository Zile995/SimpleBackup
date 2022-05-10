package com.stefan.simplebackup.data.workers

import androidx.work.*

const val REQUEST_TAG = "WORKER_TAG"
const val ARGUMENT = "PACKAGES"
const val WORK_NAME = "SIMPLE_WORK"
const val WORK_ITEMS = "NUMBER_OF_PACKAGES"

class WorkerHelper(
    private val appList: Array<String>,
    private val workManager: WorkManager
) {

    constructor(packageName: String, workManager: WorkManager) : this(
        arrayOf(packageName),
        workManager
    )

    private val constraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()

    fun startWorker(shouldBackup: Boolean = true) {
        if (shouldBackup)
            beginUniqueWork<BackupWorker>()
        else
            beginUniqueWork<RestoreWorker>()
    }

    private inline fun <reified W : ListenableWorker> beginUniqueWork() {
        OneTimeWorkRequestBuilder<W>()
            .setInputData(createInputData())
            .setConstraints(constraints)
            .addTag(REQUEST_TAG)
            .build().also { buildRequest ->
                workManager
                    .beginUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, buildRequest)
                    .enqueue()
            }
    }

    private fun createInputData(): Data {
        val builder = Data.Builder()
        builder.putStringArray(ARGUMENT, appList)
        return builder.build()
    }
}