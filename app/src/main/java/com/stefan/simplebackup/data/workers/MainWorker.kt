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
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.WorkActionBroadcastReceiver
import com.stefan.simplebackup.ui.notifications.WorkNotificationManager
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.getLastActivityIntent
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.file.FileUtil.deleteFile
import com.stefan.simplebackup.utils.file.FileUtil.tempDirPath
import com.stefan.simplebackup.utils.root.RootApkManager
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import com.stefan.simplebackup.utils.work.backup.BackupUtil
import com.stefan.simplebackup.utils.work.restore.RestoreUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import kotlin.system.measureTimeMillis

const val PROGRESS_MAX = 10_000
const val WORK_NOTIFICATION_ID = 42
const val WORK_PROGRESS = "PROGRESS"
const val NOTIFICATION_SKIP_ACTION = "NOTIFICATION_SKIP_EXTRA"
const val NOTIFICATION_CANCEL_ACTION = "NOTIFICATION_CANCEL_EXTRA"

typealias ForegroundCallback = suspend (ProgressData) -> Unit

class MainWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext, params
) {
    // Coroutine dispatchers
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    // Main coroutine scope of worker
    private var mainScope: CoroutineScope? = null

    // WorkNotificationManager
    private val workNotificationManager by lazy {
        WorkNotificationManager(context = appContext,
            notificationId = WORK_NOTIFICATION_ID,
            onClickAction = { appContext.getLastActivityIntent() },
            onSkipAction = { getPendingIntent(NOTIFICATION_SKIP_ACTION) },
            onCancelAction = { getPendingIntent(NOTIFICATION_CANCEL_ACTION) })
    }

    // Input Data
    private val workItems: Array<String>?
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
        initWorkActions()
    }

    override suspend fun doWork(): Result = supervisorScope {
        try {
            withContext(ioDispatcher) {
                var time = 0L
                mainScope = this
                mainJob = launch {
                    time = measureTimeMillis {
                        (if (shouldBackup) backup() else restore())?.collect { itemJob ->
                            workItemJob = itemJob
                        }
                    }
                }
                mainJob?.join()
                Result.success().also {
                    Log.d(
                        "MainWorker", "Work successful, completed in: ${time / 1_000.0} seconds"
                    )
                    // Delay and send new notification sound only for fast works
                    if (time <= 1_000L) delay(1_100L)
                    workItems?.apply {
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
                cleanUp()
            }
        }
    }

    private suspend fun cleanUp() {
        // Clear companion object values
        clearWorkActions()

        // Remove preference progress data
        // TODO: WIP, remove this once the progress data is saved in the repo.
        PreferenceHelper.removeProgressData()

        // Unsuspend failed apps
        workItems?.let { RootApkManager.unsuspendPackages(it) }

        // Clear progress value
        clearProgressData()
        try {
            // Delete temp dir file
            deleteFile(tempDirPath)
        } catch (e: IOException) {
            // Just log error
            Log.e("MainWorker", "Unable to delete temp dir: $e")
        }
    }

    private fun forcefullyDeleteBackup() {
        mainScope?.launch {
            ZipUtil.forcefullyDeleteZipBackups()
        }
    }

    private fun initWorkActions() {
        skipAction = {
            Log.w("MainWorker", "Clicked skip button: $workItemJob")
            val toastMessage =
                applicationContext.getString(R.string.skipping, mutableProgressData.value?.name)
            applicationContext.showToast(toastMessage)
            workItemJob?.cancel()
            forcefullyDeleteBackup()
        }

        cancelAction = {
            Log.w("MainWorker", "Clicked cancel button: $mainJob")
            val toastMessage = applicationContext.getString(R.string.canceling_work)
            applicationContext.showToast(toastMessage, true)
            mainJob?.cancel()
            forcefullyDeleteBackup()
        }
    }

    private fun getPendingIntent(actionValue: String): PendingIntent {
        val intent = Intent(applicationContext, WorkActionBroadcastReceiver::class.java).apply {
            action = actionValue
        }
        return PendingIntent.getBroadcast(
            applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun backup() = workItems?.let { backupItems ->
        val backupUtil = BackupUtil(
            appContext = applicationContext,
            backupItems = backupItems,
            updateForegroundInfo = foregroundCallBack,
            shouldBackupToCloud = shouldBackupToCloud
        )
        backupUtil.backup()
    }

    private suspend fun restore() = workItems?.let { restoreItems ->
        val restoreUtil = RestoreUtil(
            appContext = applicationContext,
            restoreItems = restoreItems,
            updateForegroundInfo = foregroundCallBack
        )
        restoreUtil.restore()
    }

    private fun setForegroundInfo(notificationId: Int): suspend (Notification) -> Unit =
        { notification ->
            setForeground(ForegroundInfo(notificationId, notification))
        }

    companion object {
        private var mainJob: Job? = null
        private var workItemJob: Job? = null

        var skipAction: (() -> Unit)? = null
            private set
        var cancelAction: (() -> Unit)? = null
            private set

        private val mutableProgressData: MutableStateFlow<ProgressData?> = MutableStateFlow(null)
        val progressData get() = mutableProgressData.asStateFlow()

        private fun clearWorkActions() {
            mainJob = null
            workItemJob = null
            skipAction = null
            cancelAction = null
        }

        private fun clearProgressData() {
            mutableProgressData.value = null
        }
    }
}

