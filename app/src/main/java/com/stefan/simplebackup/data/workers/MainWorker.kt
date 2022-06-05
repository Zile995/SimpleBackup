package com.stefan.simplebackup.data.workers

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.work.*
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.ui.notifications.WorkNotificationBuilder
import com.stefan.simplebackup.ui.notifications.WorkNotificationHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.work.backup.BackupUtil
import com.stefan.simplebackup.utils.work.restore.RestoreUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

const val PROGRESS_MAX = 10_000
const val WORK_PROGRESS = "PROGRESS"
const val WORK_ITEMS = "NUMBER_OF_PACKAGES"

typealias ForegroundCallBack = suspend (NotificationData) -> Unit

class MainWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
), WorkNotificationHelper by WorkNotificationBuilder(appContext) {

    private val items: IntArray?
        get() = inputData.getIntArray(INPUT_LIST)
    private val shouldBackup: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP, true)

    private val updateForegroundInfo = createForegroundInfo(notificationId)
    
    private val foregroundCallBack: ForegroundCallBack = { notificationData ->
        setProgress(workDataOf(WORK_PROGRESS to notificationData.progress))
        updateForegroundInfo(
            getUpdatedNotification(notificationData)
        )
    }

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                val time = measureTimeMillis {
                    if (shouldBackup) backup() else restore()
                }
                val outputData = getOutputData()
                Result.success(outputData).also {
                    Log.d("MainWorker", "Work successful, completed in: ${time / 1_000.0} seconds")
                    /**
                     *  Delay and send new notification sound only for fast works
                     */
                    if (time <= 1_000L) delay(1_000L)
                    items?.apply {
                        applicationContext.sendNotificationBroadcast(
                            notification = getFinishedNotification(
                                numberOfPackages = size,
                                isBackup = shouldBackup
                            ),
                            actionName = ACTION_WORK_FINISHED
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainWorker", "Work error: $e: ${e.message}")
            withContext(Dispatchers.Main) {
                applicationContext.showToast("Error $e: ${e.message}")
            }
            Result.failure()
        }
    }

    private suspend fun backup() {
        items?.let { backupItems ->
            val backupUtil = BackupUtil(applicationContext, backupItems, foregroundCallBack)
            backupUtil.backup()
        }
    }

    private suspend fun restore() {
        items?.let { restoreItems ->
            val restoreUtil = RestoreUtil(applicationContext, restoreItems)
            restoreUtil.restore()
        }
    }

    private fun getOutputData() = workDataOf(
        WORK_ITEMS to (items?.size ?: 0)
    )

    private fun createForegroundInfo(notificationId: Int): suspend (Notification) -> Unit {
        return { notification ->
            this.setForeground(ForegroundInfo(notificationId, notification))
        }
    }
}