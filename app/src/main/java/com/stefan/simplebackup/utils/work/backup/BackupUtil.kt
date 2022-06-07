package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import androidx.annotation.StringRes
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.data.workers.ForegroundCallBack
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import net.lingala.zip4j.exception.ZipException
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
    private val updateProgress: (Int) -> Unit = { steps ->
        currentProgress += (PROGRESS_MAX / backupItems.size) / steps
    }

    private val getResourceString: (Int) -> String = { resource ->
        appContext.getString(resource)
    }

    suspend fun backup() {
        backupItems.forEach { item ->
            repository.getAppData(item).also { app ->
                PreferenceHelper.savePackageName(app.packageName)
                app.runBackup(
                    ::createDirs,
                    ::backupData,
                    ::zipData,
                    ::serializeAppData,
                    ::moveBackup
                )
            }
        }
        PreferenceHelper.clearPackageName()
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
        setBackupTime(app)
        serializeApp(app)
        app.updateNotificationData(R.string.backup_progress_successful)
    }

    private suspend inline fun AppData.runBackup(
        vararg actions: suspend (AppData) -> Unit
    ) {
        try {
            actions.forEach { action ->
                action(this)
                updateProgress(actions.size)
            }
        } catch (exception: Exception) {
            when (exception) {
                is ZipException, is IOException -> {
                    FileUtil.deleteFile(getTempDirPath(this))
                }
            }
        }
    }
}