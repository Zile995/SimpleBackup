package com.stefan.simplebackup.data.workers

import androidx.work.*

const val WORK_NAME = "SIMPLE_WORK"
const val INPUT_LIST = "PACKAGE_LIST"
const val SHOULD_BACKUP = "WORK_TYPE"
const val WORK_REQUEST_TAG = "WORKER_TAG"
const val SHOULD_BACKUP_TO_CLOUD = "IS_CLOUD_BACKUP"

class WorkerHelper(
    private val workItems: Array<String>,
    val workManager: WorkManager
) {

    val constraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()

    inline fun <reified W : ListenableWorker> beginUniqueWork(shouldBackup: Boolean = true, dataBuilder: (Boolean) -> Data.Builder) {
        val inputData = dataBuilder(shouldBackup).build()
        OneTimeWorkRequestBuilder<W>()
            .addTag(WORK_REQUEST_TAG)
            .setConstraints(constraints)
            .setInputData(inputData)
            .build().also { buildRequest ->
                workManager
                    .beginUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, buildRequest)
                    .enqueue()
            }
    }

    inline fun <reified W : ListenableWorker> beginUniqueLocalWork(shouldBackup: Boolean = true) =
        beginUniqueWork<W>(shouldBackup, dataBuilder = {
            createLocalWorkDataBuilder(it)
        })

    inline fun <reified W : ListenableWorker> beginUniqueCloudWork() =
        beginUniqueWork<W>(true, dataBuilder = {
            createCloudWorkDataBuilder()
        })


    fun createLocalWorkDataBuilder(shouldBackup: Boolean = true): Data.Builder {
        val builder = Data.Builder()
        return builder.putStringArray(INPUT_LIST, workItems)
            .putBoolean(SHOULD_BACKUP, shouldBackup)
    }

    fun createCloudWorkDataBuilder(): Data.Builder =
        createLocalWorkDataBuilder(true)
            .putBoolean(SHOULD_BACKUP_TO_CLOUD, true)


}