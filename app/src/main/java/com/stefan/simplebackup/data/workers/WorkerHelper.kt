package com.stefan.simplebackup.data.workers

import androidx.work.*

const val REQUEST_TAG = "WORKER_TAG"
const val INPUT_LIST = "UID_LIST"
const val SHOULD_BACKUP = "WORKER_TYPE"
const val WORK_NAME = "SIMPLE_WORK"
const val WORK_ITEMS = "NUMBER_OF_PACKAGES"

class WorkerHelper(
    private val appList: IntArray,
    @PublishedApi
    internal val workManager: WorkManager
) {

    constructor(uid: Int, workManager: WorkManager) : this(
        intArrayOf(uid),
        workManager
    )

    @PublishedApi
    internal val constraints = Constraints.Builder()
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

    @PublishedApi
    internal fun createInputData(shouldBackup: Boolean): Data {
        val builder = Data.Builder()
        return builder.putIntArray(INPUT_LIST, appList)
            .putBoolean(SHOULD_BACKUP, shouldBackup)
            .build()
    }
}