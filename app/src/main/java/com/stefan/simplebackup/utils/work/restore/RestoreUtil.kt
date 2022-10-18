package com.stefan.simplebackup.utils.work.restore

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.deleteFile
import com.stefan.simplebackup.utils.file.FileUtil.getTempApkInstallDirPath
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.FileUtil.localDirPath
import com.stefan.simplebackup.utils.file.JsonUtil
import com.stefan.simplebackup.utils.root.RootApkManager
import com.stefan.simplebackup.utils.work.WorkResult
import com.stefan.simplebackup.utils.work.WorkUtil
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.coroutineScope
import java.io.IOException

class RestoreUtil(
    private val appContext: Context,
    private val restoreItems: Array<String>,
    updateForegroundInfo: ForegroundCallback
) : WorkUtil(appContext, restoreItems, updateForegroundInfo) {

    private val rootApkManager = RootApkManager(appContext)
    private val restoreApps = mutableListOf<AppData?>()

    suspend fun restore(): List<WorkResult> = coroutineScope {
        addRestoreApps()
        val results = mutableListOf<WorkResult>()
        restoreApps.forEach { restoreApp ->
            val result = restoreApp.startWork(
                ::createDirs,
                ::unzipData,
                ::installApk,
                ::restoreData
            )
            results.add(result)
        }
        results.toList()
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
        val appManager = AppManager(appContext)
        val doesExists = appManager.doesPackageExists(packageName = app.packageName)
        if (doesExists)
            rootApkManager.uninstallApk(app.packageName)
        rootApkManager.installApk(getTempApkInstallDirPath(app = app))
        rootApkManager.deleteTempInstallDir(app = app)
    }

    private suspend fun restoreData(app: AppData) {
        app.updateNotificationData(R.string.restore_progress_data_info)
        TarUtil.restoreData(app)
        deleteFile(getTempDirPath(app))
    }

    override suspend fun AppData.updateOnSuccess(): WorkResult {
        updateNotificationData(R.string.restore_progress_successful)
        return WorkResult.SUCCESS
    }

    override suspend fun AppData.updateOnFailure(): WorkResult {
        try {
            deleteFile(getTempDirPath(this))
        } catch (e: IOException) {
            Log.w("RestoreUtil", "Failed to delete broken restore files $e")
        } finally {
            setNearestItemInterval()
            updateNotificationData(R.string.restore_progress_failed)
        }
        return WorkResult.ERROR
    }
}