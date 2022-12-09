package com.stefan.simplebackup.utils.work

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.root.RootApkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.io.IOException

class RestoreUtil(
    private val appContext: Context,
    private val restoreItems: Array<String>,
    updateForegroundInfo: ForegroundCallback
) : WorkUtil(appContext, restoreItems, updateForegroundInfo) {

    // Manager classes
    private val appManager = AppManager(appContext)
    private val rootApkManager = RootApkManager(appContext)

    // List of apps that has to be restored
    private val restoreApps = mutableListOf<AppData?>()

    @WorkerThread
    suspend fun restore() = channelFlow {
        var isSkipped: Boolean
        addRestoreApps()
        supervisorScope {
            restoreApps.forEach { restoreApp ->
                isSkipped = false
                launch {
                    try {
                        restoreApp.startWork(
                            ::createDirs,
                            ::unzipData,
                            ::installApk,
                            ::restoreData
                        )
                    } catch (e: CancellationException) {
                        Log.w("RestoreUtil", "Got exception $e")
                        isSkipped = true
                    }
                }.also { itemJob ->
                    send(itemJob)
                }.join()
                if (isSkipped && restoreApp != null) onFailure(restoreApp)
            }
        }
    }

    private suspend fun addRestoreApps() {
        val jsonFiles = FileUtil.getJsonFiles(FileUtil.localDirPath) { dirName ->
            restoreItems.contains(dirName)
        }
        jsonFiles.forEach { jsonFile ->
            val restoreApp = JsonUtil.deserializeApp(jsonFile)
            restoreApps.add(restoreApp)
        }
    }

    private suspend fun createDirs(app: AppData) {
        app.updateProgressData(R.string.restore_progress_dir_info)
        FileUtil.createDirectory(tempItemDirPath)
        rootApkManager.createTempInstallDir(app)
    }

    private suspend fun unzipData(app: AppData) {
        app.updateProgressData(R.string.restore_progress_dir_info)
        ZipUtil.unzipAllData(app, backupItemDirPath)
        rootApkManager.moveApkFilesToTempDir(app)
    }

    private suspend fun installApk(app: AppData) {
        app.updateProgressData(R.string.restore_apk_install_info)
        isRestoring = true
        val appManager = AppManager(context = appContext)
        val doesExists = appManager.doesPackageExists(packageName = app.packageName)
        rootApkManager.apply {
            if (doesExists) uninstallApk(packageName = app.packageName)
            installApk(getTempApkInstallDirPath(app = app))
            deleteTempInstallDir(app = app)
            isRestoring = false
        }
    }

    private suspend fun restoreData(app: AppData) {
        app.updateProgressData(R.string.restore_progress_data_info)
        val appUid = appManager.getPackageUid(app.packageName)
        if (appUid == null) {
            app.updateProgressData(R.string.restore_progress_uid_info)
            throw IOException(appContext.getString(R.string.restore_progress_uid_info))
        }
        isRestoring = true
        TarUtil.restoreData(app, appUid)
        FileUtil.deleteFile(tempItemDirPath)
        isRestoring = false
    }

    override suspend fun onSuccess(app: AppData) {
        app.updateProgressData(R.string.restore_progress_successful, WorkResult.SUCCESS)
    }

    override suspend fun onFailure(app: AppData) {
        super.onFailure(app)
        try {
            FileUtil.deleteFile(tempItemDirPath)
            rootApkManager.deleteTempInstallDir(app)
        } catch (e: IOException) {
            Log.w("RestoreUtil", "Failed to delete broken restore files $e")
        } finally {
            app.updateProgressData(R.string.restore_progress_failed, WorkResult.ERROR)
            isRestoring = false
        }
    }

    companion object {
        var isRestoring = false
         private set
    }
}