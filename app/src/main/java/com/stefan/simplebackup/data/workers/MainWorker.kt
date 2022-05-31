package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.ui.notifications.NotificationBuilder
import com.stefan.simplebackup.ui.notifications.NotificationHelper
import com.stefan.simplebackup.utils.work.backup.BackupUtil
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.work.restore.RestoreUtil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

const val PROGRESS_MAX = 10_000
const val WORK_PROGRESS = "BackupProgress"

class MainWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
), NotificationHelper by NotificationBuilder(appContext) {

    private lateinit var outputData: Data
    private val items: IntArray?
        get() = inputData.getIntArray(INPUT_LIST)
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
                    /**
                     *  Delay and send new notification sound only for fast works
                     */
                    if (time < 1_000L) delay(1_000L)
                    items?.apply {
                        applicationContext.sendNotificationBroadcast(
                            notification = getFinishedNotification(
                                numberOfPackages = size,
                                isBackup = shouldBackup
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainWorker", "Work error: ${e.message}")
            Result.failure(outputData)
        }
    }

    private suspend fun backup() {
        items?.let { backupItems ->
            val backupUtil = BackupUtil(applicationContext, backupItems)
            backupUtil.backup().collect { notificationData ->
                updateForegroundInfo(notificationData)
            }
            outputWorkData()
        }
    }

    private suspend fun restore() {
        items?.let { restoreItems ->
            val restoreUtil = RestoreUtil(applicationContext, restoreItems)
            restoreUtil.restore()
            outputWorkData()
        }
    }

    private fun outputWorkData() {
        outputData = workDataOf(
            INPUT_LIST to true,
            WORK_ITEMS to (items?.size ?: 0)
        )
    }

    private suspend fun updateForegroundInfo(
        notificationData: NotificationData,
    ) {
        notificationBuilder.apply {
            setProgress(workDataOf(WORK_PROGRESS to notificationData.progress))
            setForeground(
                ForegroundInfo(
                    notificationId,
                    notificationBuilder
                        .updateNotificationContent(notificationData)
                        .setProgress(PROGRESS_MAX, notificationData.progress, false)
                        .build()
                )
            )
        }
    }
}