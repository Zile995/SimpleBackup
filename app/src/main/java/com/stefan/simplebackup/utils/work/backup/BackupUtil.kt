package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.ForegroundCallBack
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
class BackupUtil(
    appContext: Context,
    private val backupItems: IntArray,
    private val updateForegroundInfo: ForegroundCallBack
) : FileHelper {

    private val notificationData = NotificationData()
    private val repository = (appContext as MainApplication).getRepository

    private var currentProgress = 0
    private val generatedIntervals = mutableListOf<Int>()
    private val perItemInterval = PROGRESS_MAX / backupItems.size

    private val updateProgress = { steps: Int ->
        currentProgress += perItemInterval / steps
    }

    private val getResourceString: (Int) -> String = { resource ->
        appContext.getString(resource)
    }

    init {
        generateIntervals(backupItems.size)
    }

    suspend fun backup(): List<WorkResult> {
        val results = mutableListOf<WorkResult>()
        backupItems.forEach { item ->
            repository.getAppData(item).also { app ->
                PreferenceHelper.savePackageName(app.packageName)
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
        PreferenceHelper.clearPackageName()
        return results.toList()
    }

    private suspend fun createDirs(app: AppData) {
        app.updateNotificationData(R.string.backup_progress_dir_info)
        createMainDir()
        createAppBackupDir(getTempDirPath(app))
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
        app.updateNotificationData(R.string.backup_progress_saving_application_data)
        setBackupTime(app)
        serializeApp(app)
    }

    private suspend inline fun AppData.runBackup(
        vararg actions: suspend (AppData) -> Unit
    ): WorkResult {
        return try {
            actions.forEach { action ->
                action(this)
                updateProgress(actions.size)
            }
            this.updateNotificationData(R.string.backup_progress_successful)
            WorkResult.SUCCESS
        } catch (exception: IOException) {
            Log.e("BackupUtil", "Oh, an error occurred: $exception ${exception.localizedMessage}")
            FileUtil.deleteFile(getTempDirPath(this))
            setNearestItemInterval(this)
            WorkResult.ERROR
        }
    }


    private fun generateIntervals(numberOfItems: Int) {
        var intervalSum = 0
        repeat(numberOfItems) {
            intervalSum += perItemInterval
            generatedIntervals.add(intervalSum)
        }
    }

    private suspend fun setNearestItemInterval(app: AppData) {
        currentProgress = generatedIntervals.first { interval ->
            interval > currentProgress
        }
        app.updateNotificationData(R.string.backup_progress_failed)
    }

    private suspend fun AppData.updateNotificationData(@StringRes info: Int) {
        val app = this
        notificationData.apply {
            name = app.name
            image = app.bitmap
            progress = currentProgress
            text = getResourceString(info)
        }
        updateForegroundInfo(notificationData)
    }
}

enum class WorkResult {
    SUCCESS,
    ERROR
}