package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.file.FileUtil.createDirectory
import com.stefan.simplebackup.utils.file.FileUtil.deleteDirectoryFiles
import com.stefan.simplebackup.utils.file.FileUtil.deleteFile
import com.stefan.simplebackup.utils.file.FileUtil.getBackupDirPath
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.FileUtil.moveFiles
import com.stefan.simplebackup.utils.work.WorkResult
import com.stefan.simplebackup.utils.work.WorkUtil
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.IOException

class BackupUtil(
    private val appContext: Context,
    private val backupItems: Array<String>,
    updateForegroundInfo: ForegroundCallback
) : WorkUtil(appContext, backupItems, updateForegroundInfo) {

    suspend fun backup(): List<WorkResult> = coroutineScope {
        val results = mutableListOf<WorkResult>()
        val database = AppDatabase.getInstance(appContext, this)
        val repository = AppRepository(database.appDao())
        backupItems.forEach { item ->
            repository.getAppData(item).also { app ->
                val result = app.startWork(
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
        createDirectory(getTempDirPath(app))
        createDirectory(getBackupDirPath(app))
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
            serializeApp(getTempDirPath(app = this))
        }
    }

    private suspend fun moveBackup(app: AppData) {
        val tempDirFile = File(getTempDirPath(app))
        val localDirFile = File(getBackupDirPath(app))
        deleteDirectoryFiles(localDirFile)
        moveFiles(sourceDir = tempDirFile, targetFile = localDirFile)
    }

    override fun updateWhenAppDoesNotExists(): WorkResult {
        appContext.showToast(R.string.app_does_not_exist)
        setNearestItemInterval()
        return WorkResult.ERROR
    }

    override suspend fun AppData.updateOnSuccess(): WorkResult {
        updateNotificationData(R.string.backup_progress_successful)
        return WorkResult.SUCCESS
    }

    override suspend fun AppData.updateOnFailure(): WorkResult {
        try {
            deleteFile(getTempDirPath(this))
        } catch (e: IOException) {
            Log.w("BackupUtil", "Failed to delete broken backup $e ${e.message}")
        } finally {
            setNearestItemInterval()
            updateNotificationData(R.string.backup_progress_failed)
        }
        return WorkResult.ERROR
    }
}