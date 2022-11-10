package com.stefan.simplebackup.utils.work.restore

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.deleteFile
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.FileUtil.localDirPath
import com.stefan.simplebackup.utils.file.JsonUtil
import com.stefan.simplebackup.utils.root.RootApkManager
import com.stefan.simplebackup.utils.work.WorkResult
import com.stefan.simplebackup.utils.work.WorkUtil
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import java.io.IOException

class RestoreUtil(
    private val appContext: Context,
    private val restoreItems: Array<String>,
    updateForegroundInfo: ForegroundCallback
) : WorkUtil(appContext, restoreItems, updateForegroundInfo) {

    private val appManager = AppManager(appContext)
    private val rootApkManager = RootApkManager(appContext)
    private val restoreApps = mutableListOf<AppData?>()

    suspend fun restore() = channelFlow {
        addRestoreApps()
        restoreApps.forEach { restoreApp ->
            launch {
                try {
                    restoreApp.startWork(
                        ::createDirs,
                        ::unzipData,
                        ::installApk,
                        ::restoreData
                    )
                } catch (e: Exception) {
                    Log.w("RestoreUtil", "Got exception $e")
                    if (restoreApp != null) {
                        onFailure(restoreApp)
                    }
                }
            }.also { perItemJob ->
                send(perItemJob)
            }.join()
        }
    }

    private suspend fun addRestoreApps() {
        val jsonFiles = FileUtil.getJsonFiles(localDirPath) { dirName ->
            restoreItems.contains(dirName)
        }
        jsonFiles.forEach { jsonFile ->
            val restoreApp = JsonUtil.deserializeApp(jsonFile)
            restoreApps.add(restoreApp)
        }
    }

    private suspend fun createDirs(app: AppData) {
        app.updateNotificationData(R.string.restore_progress_dir_info)
        FileUtil.createDirectory(getTempDirPath(app))
        rootApkManager.createTempInstallDir(app)
    }

    private suspend fun unzipData(app: AppData) {
        app.updateNotificationData(R.string.restore_progress_dir_info)
        ZipUtil.unzipAllData(app)
        rootApkManager.moveApkFilesToTempDir(app)
    }

    private suspend fun installApk(app: AppData) {
        app.updateNotificationData(R.string.restore_apk_install_info)
        val appManager = AppManager(context = appContext)
        val doesExists = appManager.doesPackageExists(packageName = app.packageName)
        rootApkManager.apply {
            if (doesExists) uninstallApk(packageName = app.packageName)
            installApk(getTempApkInstallDirPath(app = app))
            deleteTempInstallDir(app = app)
        }
    }

    private suspend fun restoreData(app: AppData) {
        app.updateNotificationData(R.string.restore_progress_data_info)
        val appUid = appManager.getPackageUid(app.packageName)
        if (appUid == null) {
            app.updateNotificationData(R.string.restore_progress_uid_info)
            throw IOException(appContext.getString(R.string.restore_progress_uid_info))
        }
        TarUtil.restoreData(app, appUid)
        deleteFile(getTempDirPath(app))
    }

    override suspend fun onSuccess(app: AppData) {
        app.updateNotificationData(R.string.restore_progress_successful, WorkResult.SUCCESS)
    }

    override suspend fun onFailure(app: AppData) {
        super.onFailure(app)
        try {
            deleteFile(getTempDirPath(app))
        } catch (e: IOException) {
            Log.w("RestoreUtil", "Failed to delete broken restore files $e")
        } finally {
            app.updateNotificationData(R.string.restore_progress_failed, WorkResult.ERROR)
        }
    }
}