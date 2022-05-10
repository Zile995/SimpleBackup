package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.notifications.NotificationBuilder
import com.stefan.simplebackup.ui.notifications.NotificationHelper
import com.stefan.simplebackup.utils.backup.BackupUtil
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

const val BACKUP_PROGRESS = "BackupProgress"
const val PROGRESS_MAX = 10_000

class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
), NotificationHelper by NotificationBuilder(appContext) {

    private lateinit var outputData: Data
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val packageNames: Array<String>?
        get() = inputData.getStringArray(ARGUMENT)

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                outputData = workDataOf(ARGUMENT to false)
                val time = measureTimeMillis {
                    backup()
                }
                Log.d("BackupWorker", "Backup successful, completed in: ${time / 1000.0} seconds")
                Result.success(outputData).also {
                    delay(1_000L)
                    packageNames?.apply {
                        applicationContext.sendNotificationBroadcast(
                            notification = getFinishedNotification(numberOfPackages = size),
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error: ${e.message}")
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
            ARGUMENT to true,
            WORK_ITEMS to (packageNames?.size ?: 0)
        )
    }

    private suspend fun updateForegroundInfo(
        currentProgress: Int,
        app: AppData
    ) {
        notificationBuilder.apply {
            setProgress(workDataOf(BACKUP_PROGRESS to currentProgress))
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