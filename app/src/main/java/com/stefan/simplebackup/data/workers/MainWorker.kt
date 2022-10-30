package com.stefan.simplebackup.data.workers

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.ui.notifications.WorkNotificationBuilder
import com.stefan.simplebackup.ui.notifications.WorkNotificationHelper
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.work.backup.BackupUtil
import com.stefan.simplebackup.utils.work.restore.RestoreUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.measureTimeMillis

const val PROGRESS_MAX = 10_000
const val WORK_PROGRESS = "PROGRESS"
const val WORK_ITEMS = "NUMBER_OF_PACKAGES"

typealias ForegroundCallback = suspend (ProgressData) -> Unit

class MainWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
), WorkNotificationHelper by WorkNotificationBuilder(appContext) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    private val items: Array<String>?
        get() = inputData.getStringArray(INPUT_LIST)
    private val shouldBackup: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP, true)
    private val shouldBackupToCloud: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP_TO_CLOUD, false)

    private val updateForegroundInfo = setForegroundInfo(notificationId)

    private val foregroundCallBack: ForegroundCallback = { progressData ->
        _progressData.value = progressData
        updateForegroundInfo(getUpdatedNotification(progressData, shouldBackup))
        setProgress(workDataOf(WORK_PROGRESS to progressData.progress))
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
                    if (time <= 1_000L) delay(1_100L)
                    items?.apply {
                        applicationContext.sendNotificationBroadcast(
                            notification = getFinishedNotification(
                                isBackupNotification = shouldBackup
                            ),
                            actionName = ACTION_WORK_FINISHED
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainWorker", "Work error: $e")
            withContext(mainDispatcher) {
                applicationContext.showToast("Error $e", true)
            }
            Result.failure()
        }
    }

    private suspend fun backup() =
        items?.let { backupItems ->
            val backupUtil = BackupUtil(
                appContext = applicationContext,
                backupItems = backupItems,
                updateForegroundInfo = foregroundCallBack,
                shouldBackupToCloud = shouldBackupToCloud
            )
            backupUtil.backup()
        }

    private suspend fun restore() =
        items?.let { restoreItems ->
            val restoreUtil = RestoreUtil(
                appContext = applicationContext,
                restoreItems = restoreItems,
                updateForegroundInfo = foregroundCallBack
            )
            restoreUtil.restore()
        }

    private fun getOutputData() = workDataOf(WORK_ITEMS to (items?.size ?: 0))

    private fun setForegroundInfo(notificationId: Int): suspend (Notification) -> Unit =
        { notification ->
            setForeground(ForegroundInfo(notificationId, notification))
        }

    companion object ProgressObserver {
        private var _progressData: MutableStateFlow<ProgressData?> =
            MutableStateFlow(null)

        val progressData get() = _progressData.asStateFlow()

        fun clearProgressData() {
            _progressData.value = null
        }
    }
}