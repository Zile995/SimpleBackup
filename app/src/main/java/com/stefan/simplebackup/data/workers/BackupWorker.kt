package com.stefan.simplebackup.data.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.stefan.simplebackup.data.broadcasts.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.broadcasts.NotificationBroadcastReceiver
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION
import com.stefan.simplebackup.ui.notifications.NotificationBuilder
import com.stefan.simplebackup.utils.backup.ARGUMENT
import com.stefan.simplebackup.utils.backup.WORK_ITEMS
import com.stefan.simplebackup.utils.backup.BackupUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

const val BACKUP_PROGRESS = "BackupProgress"
const val PROGRESS_MAX = 10_000

class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {

    private lateinit var outputData: Data
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val notificationBuilder = NotificationBuilder(appContext, true)
    private val packageNames: Array<String>?
        get() = inputData.getStringArray(ARGUMENT)

    override suspend fun doWork(): Result = coroutineScope {
        try {
            outputData = workDataOf(ARGUMENT to false)
            withContext(ioDispatcher) {
                val time = measureTimeMillis {
                    backup()
                }
                Log.d("BackupWorker", "Backup successful, completed in: ${time / 1000.0} seconds")
                Result.success(outputData).also {
                    sendNotificationBroadcast()
                }
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error + ${e.message}")
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

    private fun sendNotificationBroadcast() {
        applicationContext.sendBroadcast(
            Intent(
                applicationContext,
                NotificationBroadcastReceiver::class.java
            ).apply {
                action = ACTION_WORK_FINISHED
                putExtra(WORK_ITEMS, packageNames?.size ?: 0)
                putExtra(
                    EXTRA_NOTIFICATION,
                    notificationBuilder
                        .getBackupFinishedNotification(packageNames?.size ?: 0)
                )
                setPackage(applicationContext.packageName)
            })
    }

    private suspend fun updateForegroundInfo(
        currentProgress: Int,
        app: AppData
    ) {
        notificationBuilder.apply {
            setProgress(workDataOf(BACKUP_PROGRESS to currentProgress))
            setForeground(
                ForegroundInfo(
                    getNotificationId,
                    getBuilder
                        .updateNotificationContent(app)
                        .setProgress(PROGRESS_MAX, currentProgress, false)
                        .build()
                )
            )
        }
    }
}