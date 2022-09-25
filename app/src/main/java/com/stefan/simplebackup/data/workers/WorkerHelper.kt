package com.stefan.simplebackup.data.workers

import androidx.work.*

const val WORK_NAME = "SIMPLE_WORK"
const val REQUEST_TAG = "WORKER_TAG"
const val INPUT_LIST = "PACKAGE_LIST"
const val SHOULD_BACKUP = "WORKER_TYPE"

class WorkerHelper(
    private val workItems: Array<String>,
    val workManager: WorkManager
) {

    constructor(packageName: String, workManager: WorkManager) : this(
        arrayOf(packageName),
        workManager
    )

    val constraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()

    inline fun <reified W : ListenableWorker> beginUniqueWork(shouldBackup: Boolean = true) {
        val inputData = createInputData(shouldBackup)
        OneTimeWorkRequestBuilder<W>()
            .addTag(REQUEST_TAG)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build().also { buildRequest ->
                workManager
                    .beginUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, buildRequest)
                    .enqueue()
            }
    }

    fun createInputData(shouldBackup: Boolean): Data {
        val dataPreference = mapOf<String, Array<Boolean>>()
        val builder = Data.Builder()
        return builder.putStringArray(INPUT_LIST, workItems)
            .putAll(dataPreference)
            .putBoolean(SHOULD_BACKUP, shouldBackup)
            .build()
    }
}