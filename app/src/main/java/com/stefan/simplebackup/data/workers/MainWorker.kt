package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.notifications.NotificationBuilder
import com.stefan.simplebackup.ui.notifications.NotificationHelper
import com.stefan.simplebackup.utils.backup.BackupUtil
import com.stefan.simplebackup.utils.restore.RestoreUtil
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

const val WORK_PROGRESS = "BackupProgress"
const val PROGRESS_MAX = 10_000

class MainWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
), NotificationHelper by NotificationBuilder(appContext) {

    private lateinit var outputData: Data
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val packageNames: Array<String>?
        get() = inputData.getStringArray(INPUT_LIST)
    private val shouldBackup: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP, true)

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                outputData = workDataOf(INPUT_LIST to false)
                val time = measureTimeMillis {
                    if (shouldBackup) backup() else restore()
                }
                Result.success(outputData).also {
                    Log.d("MainWorker", "Work successful, completed in: ${time / 1_000.0} seconds")
                    delay(1_000L)
                    packageNames?.apply {
                        applicationContext.sendNotificationBroadcast(
                            notification = getFinishedNotification(
                                numberOfPackages = size,
                                isBackup = shouldBackup
                            ),
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainWorker", "Work error: ${e.message}")
            Result.failure(outputData)
        }
    }

    private suspend fun backup() {
        packageNames?.let { packageNames ->
            val backupUtil = BackupUtil(applicationContext, packageNames)
            backupUtil.backup().collect { progressInfo ->
                updateForegroundInfo(progressInfo.first, progressInfo.second)
            }
        }
        outputData = workDataOf(
            INPUT_LIST to true,
            WORK_ITEMS to (packageNames?.size ?: 0)
        )
    }

    private suspend fun restore() {
        packageNames?.let { packageNames ->
            val restoreUtil = RestoreUtil(applicationContext, packageNames)
            restoreUtil.restore()
        }
    }

    private suspend fun updateForegroundInfo(
        currentProgress: Int,
        app: AppData
    ) {
        notificationBuilder.apply {
            setProgress(workDataOf(WORK_PROGRESS to currentProgress))
            setForeground(
                ForegroundInfo(
                    notificationId,
                    notificationBuilder
                        .updateNotificationContent(app)
                        .setProgress(PROGRESS_MAX, currentProgress, false)
                        .build()
                )
            )
        }
    }
}