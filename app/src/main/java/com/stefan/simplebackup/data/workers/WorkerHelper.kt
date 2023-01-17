package com.stefan.simplebackup.data.workers

import androidx.work.*
import java.util.concurrent.TimeUnit

const val WORK_NAME = "SIMPLE_WORK"
const val INPUT_LIST = "PACKAGE_LIST"
const val SHOULD_BACKUP = "WORK_TYPE"
const val WORK_REQUEST_TAG = "WORKER_TAG"
const val SHOULD_BACKUP_TO_CLOUD = "IS_CLOUD_BACKUP"

class WorkerHelper(
    private val workItems: Array<String>,
    private val workManager: WorkManager
) {

    inline fun <reified W : ListenableWorker> createOneTimeWorkRequest(
        workTag: String,
        inputData: Data,
        constraints: Constraints,
        resultsDurationInMinutes: Long,
    ) = OneTimeWorkRequestBuilder<W>()
        .addTag(workTag)
        .setConstraints(constraints)
        .keepResultsForAtLeast(resultsDurationInMinutes, TimeUnit.MINUTES)
        .setInputData(inputData)
        .build()

    private inline fun beginUniqueMainWork(
        shouldBackup: Boolean = true,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        dataBuilder: (Boolean) -> Data.Builder
    ) {
        val inputData = dataBuilder(shouldBackup).build()
        createOneTimeWorkRequest<MainWorker>(
            workTag = WORK_REQUEST_TAG,
            inputData = inputData,
            constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build(),
            resultsDurationInMinutes = 30).also { workRequest ->
            workManager
                .beginUniqueWork(WORK_NAME, existingWorkPolicy, workRequest)
                .enqueue()
        }
    }

    fun beginUniqueLocalWork(shouldBackup: Boolean = true) =
        beginUniqueMainWork(shouldBackup, dataBuilder = { getLocalWorkDataBuilder(shouldBackup) })

    fun beginUniqueCloudBackupWork() =
        beginUniqueMainWork(shouldBackup = true, dataBuilder = { getCloudWorkDataBuilder() })

    private fun getLocalWorkDataBuilder(shouldBackup: Boolean = true): Data.Builder {
        val builder = Data.Builder()
        return builder.putStringArray(INPUT_LIST, workItems)
            .putBoolean(SHOULD_BACKUP, shouldBackup)
    }

    private fun getCloudWorkDataBuilder(): Data.Builder =
        getLocalWorkDataBuilder(true)
            .putBoolean(SHOULD_BACKUP_TO_CLOUD, true)
}