package com.stefan.simplebackup.data.workers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.WorkActionBroadcastReceiver
import com.stefan.simplebackup.ui.notifications.WorkNotificationManager
import com.stefan.simplebackup.utils.extensions.getLastActivityIntent
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.file.FileUtil.deleteDirectoryFiles
import com.stefan.simplebackup.utils.file.FileUtil.tempDirPath
import com.stefan.simplebackup.utils.work.backup.BackupUtil
import com.stefan.simplebackup.utils.work.restore.RestoreUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.system.measureTimeMillis

const val PROGRESS_MAX = 10_000
const val WORK_NOTIFICATION_ID = 42
const val WORK_PROGRESS = "PROGRESS"
const val WORK_ITEMS = "NUMBER_OF_PACKAGES"
const val NOTIFICATION_SKIP_ACTION = "NOTIFICATION_SKIP_EXTRA"
const val NOTIFICATION_CANCEL_ACTION = "NOTIFICATION_CANCEL_EXTRA"

typealias ForegroundCallback = suspend (ProgressData) -> Unit

class MainWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext, params
) {
    // Coroutine dispatchers
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    // WorkNotificationManager
    private val workNotificationManager by lazy {
        WorkNotificationManager(
            context = appContext,
            notificationId = WORK_NOTIFICATION_ID,
            onClickAction = { appContext.getLastActivityIntent() },
            onSkipAction = { getPendingIntent(NOTIFICATION_SKIP_ACTION) },
            onCancelAction = { getPendingIntent(NOTIFICATION_CANCEL_ACTION) })
    }

    // Input Data
    private val items: Array<String>?
        get() = inputData.getStringArray(INPUT_LIST)
    private val shouldBackup: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP, true)
    private val shouldBackupToCloud: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP_TO_CLOUD, false)

    // Foreground info lambdas
    private val updateForegroundInfo = setForegroundInfo(workNotificationManager.notificationId)
    private val foregroundCallBack: ForegroundCallback = { progressData ->
        mutableProgressData.value = progressData
        val updatedNotification =
            workNotificationManager.getUpdatedNotification(progressData, shouldBackup)
        updateForegroundInfo(updatedNotification)
        setProgress(workDataOf(WORK_PROGRESS to progressData.progress))
    }

    init {
        initNotificationActions()
    }

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                var time = 0L
                mainJob = launch {
                    time = measureTimeMillis {
                        (if (shouldBackup) backup() else restore())?.collect { currentItemJob ->
                            perItemJob = currentItemJob
                        }
                    }
                }
                mainJob?.join()
                val outputData = getOutputData()
                Result.success(outputData).also {
                    Log.d(
                        "MainWorker", "Work successful, completed in: ${time / 1_000.0} seconds"
                    )
                    /**
                     *  Delay and send new notification sound only for fast works
                     */
                    if (time <= 1_000L) delay(1_100L)
                    items?.apply {
                        workNotificationManager.sendNotificationBroadcast(
                            context = applicationContext,
                            notification = workNotificationManager.getFinishedNotification(
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
        } finally {
            launch {
                clearWorkerActions()
                deleteDirectoryFiles(File(tempDirPath))
            }
        }
    }

    private fun initNotificationActions() {
        skipAction = {
            Log.w("MainWorker", "Clicked skip button: $perItemJob")
            perItemJob?.cancel()
        }

        cancelAction = {
            Log.w("MainWorker", "Clicked cancel button: $mainJob")
            mainJob?.cancel()
        }
    }

    private fun getPendingIntent(actionValue: String): PendingIntent {
        val intent = Intent(applicationContext, WorkActionBroadcastReceiver::class.java).apply {
            action = actionValue
        }
        return PendingIntent
            .getBroadcast(
                applicationContext,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
    }

    private suspend fun backup() = items?.let { backupItems ->
        val backupUtil = BackupUtil(
            appContext = applicationContext,
            backupItems = backupItems,
            updateForegroundInfo = foregroundCallBack,
            shouldBackupToCloud = shouldBackupToCloud
        )
        backupUtil.backup()
    }

    private suspend fun restore() = items?.let { restoreItems ->
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

    companion object {
        private var mainJob: Job? = null
        private var perItemJob: Job? = null

        var skipAction: (() -> Unit)? = null
            private set
        var cancelAction: (() -> Unit)? = null
            private set

        private val mutableProgressData: MutableStateFlow<ProgressData?> = MutableStateFlow(null)
        val progressData get() = mutableProgressData.asStateFlow()

        private fun clearWorkerActions() {
            skipAction = null
            cancelAction = null
            mainJob = null
            perItemJob = null
        }

        fun clearProgressData() {
            mutableProgressData.value = null
        }
    }
}

