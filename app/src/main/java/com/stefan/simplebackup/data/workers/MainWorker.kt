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
import com.google.api.services.drive.Drive
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.ProgressRepository
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.WorkActionBroadcastReceiver
import com.stefan.simplebackup.ui.notifications.WorkNotificationManager
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.root.RootApkManager
import com.stefan.simplebackup.utils.work.BackupUtil
import com.stefan.simplebackup.utils.work.FileUtil
import com.stefan.simplebackup.utils.work.FileUtil.deleteFile
import com.stefan.simplebackup.utils.work.FileUtil.tempDirPath
import com.stefan.simplebackup.utils.work.RestoreUtil
import com.stefan.simplebackup.utils.work.TEMP_DIR_NAME
import kotlinx.coroutines.*
import java.io.File
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
    // Main coroutine scope of worker
    private var mainScope: CoroutineScope? = null

    // Coroutine dispatchers
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    // Input Data
    private val workItems: Array<String>?
        get() = inputData.getStringArray(INPUT_LIST)
    private val shouldBackup: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP, true)
    private val shouldBackupToCloud: Boolean
        get() = inputData.getBoolean(SHOULD_BACKUP_TO_CLOUD, false)

    // WorkNotificationManager
    private val workNotificationManager by lazy {
        WorkNotificationManager(context = appContext,
            notificationId = WORK_NOTIFICATION_ID,
            onClickAction = { appContext.getLaunchPendingIntent() },
            onSkipAction = { getPendingIntent(NOTIFICATION_SKIP_ACTION) },
            onCancelAction = { getPendingIntent(NOTIFICATION_CANCEL_ACTION) })
    }

    private lateinit var progressRepository: ProgressRepository

    // Foreground info lambdas
    private val updateForegroundInfo = setForegroundInfo(workNotificationManager.notificationId)
    private val foregroundCallBack: ForegroundCallback = { progressData ->
        progressRepository.insert(progressData)
        val updatedNotification =
            workNotificationManager.getUpdatedNotification(progressData, shouldBackup)
        updateForegroundInfo(updatedNotification)
        setProgress(workDataOf(WORK_PROGRESS to progressData.progress))
    }

    // Drive service
    private val driveService: Drive? by lazy {
        if (shouldBackupToCloud)
            applicationContext.getDriveService(applicationContext.getLastSignedInAccount())
        else null
    }

    init {
        initWorkActions()
    }

    override suspend fun doWork(): Result = supervisorScope {
        try {
            withContext(ioDispatcher) {
                var time = 0L
                mainScope = this
                val database = AppDatabase.getInstance(applicationContext, this)
                progressRepository = ProgressRepository(database.progressDao())
                mainJob = launch {
                    time = measureTimeMillis {
                        startMainWork()?.collect { itemJob ->
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
                    workNotificationManager.sendNotificationBroadcast(
                        context = applicationContext,
                        notification = workNotificationManager.getFinishedNotification(
                            isBackupNotification = shouldBackup,
                            numOfWorkItems = workItems!!.size
                        ),
                        actionName = ACTION_WORK_FINISHED
                    )
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

    private fun canInterrupt(): Boolean {
        if (shouldBackup) return true
        val canInterrupt = !shouldBackup && RestoreUtil.canInterrupt
        if (!canInterrupt)
            applicationContext.showToast(applicationContext.getString(R.string.restoring_please_wait))
        return canInterrupt
    }

    private fun initWorkActions() {
        skipAction = {
            Log.w("MainWorker", "Clicked skip button: $workItemJob")
            if (canInterrupt()) {
                val toastMessage =
                    applicationContext.getString(R.string.skipping)
                applicationContext.showToast(toastMessage)
                workItemJob?.cancel()
                forcefullyDeleteBackup()
            }
        }

        cancelAction = {
            if (canInterrupt()) {
                Log.w("MainWorker", "Clicked cancel button: $mainJob")
                val toastMessage = applicationContext.getString(R.string.canceling_work)
                applicationContext.showToast(toastMessage, true)
                mainJob?.cancel()
                forcefullyDeleteBackup()
            }
        }
    }

    private suspend fun startMainWork() = if (shouldBackup) backup() else restore()

    private suspend fun backup() =
        workItems?.let { backupItems ->
            val backupUtil = BackupUtil(
                appContext = applicationContext,
                backupItems = backupItems,
                updateForegroundInfo = foregroundCallBack,
                shouldBackupToCloud = shouldBackupToCloud
            )
            backupUtil.backup()
        }

    private suspend fun restore() =
        workItems?.let { restoreItems ->
            val restoreUtil = RestoreUtil(
                appContext = applicationContext,
                restoreItems = restoreItems,
                updateForegroundInfo = foregroundCallBack
            )
            restoreUtil.restore()
        }

    private fun forcefullyDeleteBackup() {
        mainScope?.launch {
            runBlocking(coroutineContext) {
                try {
                    if (BackupUtil.isZippingData) {
                        val tempDirFile = File(tempDirPath)
                        FileUtil.deleteDirectoryFiles(tempDirFile)
                    }
                    forcefullyDeleteCloudBackup()
                } catch (e: IOException) {
                    // Just log and continue executing
                    Log.w("ZipUtil", "Exception while deleting files in dir $e")
                }
            }
        }
    }

    private suspend fun forcefullyDeleteCloudBackup() {
        if (shouldBackupToCloud)
            driveService?.deleteFile(fileName = TEMP_DIR_NAME)
    }

    private fun getPendingIntent(actionValue: String): PendingIntent {
        val intent = Intent(applicationContext, WorkActionBroadcastReceiver::class.java).apply {
            action = actionValue
        }
        return PendingIntent.getBroadcast(
            applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setForegroundInfo(notificationId: Int): suspend (Notification) -> Unit =
        { notification ->
            setForeground(ForegroundInfo(notificationId, notification))
        }

    private suspend fun cleanUp() {
        // Clear companion object values
        clearWorkActions()

        // Unsuspend failed apps
        workItems?.let { RootApkManager.unsuspendPackages(it) }
        try {
            // Delete temp dir file
            deleteFile(tempDirPath)
            driveService?.deleteFile(fileName = TEMP_DIR_NAME)
        } catch (e: IOException) {
            // Just log error
            Log.e("MainWorker", "Unable to delete temp dir: $e")
        }
    }

    companion object {
        private var mainJob: Job? = null
        private var workItemJob: Job? = null

        var skipAction: (() -> Unit)? = null
            private set
        var cancelAction: (() -> Unit)? = null
            private set

        private fun clearWorkActions() {
            mainJob = null
            workItemJob = null
            skipAction = null
            cancelAction = null
        }
    }
}