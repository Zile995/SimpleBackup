package com.stefan.simplebackup.data.workers

import androidx.work.*

const val REQUEST_TAG = "WORKER_TAG"
const val INPUT_LIST = "UID_LIST"
const val SHOULD_BACKUP = "WORKER_TYPE"
const val WORK_NAME = "SIMPLE_WORK"

class WorkerHelper(
    private val workItems: IntArray,
    val workManager: WorkManager
) {

    constructor(uid: Int, workManager: WorkManager) : this(
        intArrayOf(uid),
        workManager
    )

    val constraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()

    inline fun <reified W : ListenableWorker> beginUniqueWork(shouldBackup: Boolean = true) {
        OneTimeWorkRequestBuilder<W>()
            .setInputData(createInputData(shouldBackup))
            .setConstraints(constraints)
            .addTag(REQUEST_TAG)
            .build().also { buildRequest ->
                workManager
                    .beginUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, buildRequest)
                    .enqueue()
            }
    }

    fun createInputData(shouldBackup: Boolean): Data {
        val builder = Data.Builder()
        return builder.putIntArray(INPUT_LIST, workItems)
            .putBoolean(SHOULD_BACKUP, shouldBackup)
            .build()
    }
}