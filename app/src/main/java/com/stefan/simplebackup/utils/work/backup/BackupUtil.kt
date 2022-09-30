package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.createDirectory
import com.stefan.simplebackup.utils.file.FileUtil.createMainDir
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.FileUtil.moveBackup
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.coroutineScope
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
class BackupUtil(
    private val appContext: Context,
    private val backupItems: Array<String>,
    private val updateForegroundInfo: ForegroundCallback
) {
    // Progress variables
    private var currentProgress = 0
    private val generatedIntervals = mutableListOf<Int>()
    private val perItemInterval = PROGRESS_MAX / backupItems.size
    private val updateProgress = { steps: Int ->
        currentProgress += perItemInterval / steps
    }

    init {
        generateIntervals()
    }

    suspend fun backup(): List<WorkResult> = coroutineScope {
        val results = mutableListOf<WorkResult>()
        val database = AppDatabase.getInstance(appContext)
        val repository = AppRepository(database.appDao())
        backupItems.forEach { item ->
            repository.getAppData(item).also { app ->
                val result = app.runBackup(
                    ::createDirs,
                    ::backupData,
                    ::zipData,
                    ::serializeAppData,
                    ::moveBackup
                )
                results.add(result)
            }
        }
        results.toList()
    }

    private suspend fun createDirs(app: AppData) {
        app.updateNotificationData(R.string.backup_progress_dir_info)
        createMainDir()
        createDirectory(getTempDirPath(app))
    }

    private suspend fun backupData(app: AppData) {
        app.updateNotificationData(R.string.backup_progress_data_info)
        TarUtil.backupData(app)
    }

    private suspend fun zipData(app: AppData) {
        app.updateNotificationData(R.string.backup_progress_zip_info)
        ZipUtil.zipAllData(app)
    }

    private suspend fun serializeAppData(app: AppData) {
        app.apply {
            updateNotificationData(R.string.backup_progress_saving_application_data)
            setCurrentDate()
            serializeApp()
        }
    }

    private suspend inline fun AppData?.runBackup(
        vararg actions: suspend (AppData) -> Unit
    ): WorkResult {
        return when {
            this == null -> {
                updateWhenAppDoesNotExists()
            }
            else -> {
                try {
                    actions.forEach { action ->
                        action(this)
                        updateProgress(actions.size)
                    }
                    updateOnSuccess()
                } catch (exception: IOException) {
                    Log.e(
                        "BackupUtil",
                        "Oh, an error occurred: $exception ${exception.localizedMessage}"
                    )
                    updateOnFailure()
                }
            }
        }
    }

    private fun updateWhenAppDoesNotExists(): WorkResult {
        appContext.showToast(R.string.app_does_not_exist)
        setNearestItemInterval()
        return WorkResult.ERROR
    }

    private suspend fun AppData.updateOnSuccess(): WorkResult {
        updateNotificationData(R.string.backup_progress_successful)
        return WorkResult.SUCCESS
    }

    private suspend fun AppData.updateOnFailure(): WorkResult {
        FileUtil.deleteFile(getTempDirPath(this))
        setNearestItemInterval()
        updateNotificationData(R.string.backup_progress_failed)
        return WorkResult.ERROR
    }

    private fun generateIntervals() {
        var intervalSum = 0
        repeat(backupItems.size) {
            intervalSum += perItemInterval
            generatedIntervals.add(intervalSum)
        }
    }

    private fun setNearestItemInterval() {
        currentProgress = generatedIntervals.first { interval ->
            interval > currentProgress
        }
    }

    private suspend fun AppData.updateNotificationData(@StringRes info: Int) {
        val text = appContext.getString(info)
        val notificationData =
            NotificationData(
                name = name,
                text = text,
                image = bitmap,
                progress = currentProgress
            )
        updateForegroundInfo(notificationData)
    }
}

enum class WorkResult {
    SUCCESS,
    ERROR
}