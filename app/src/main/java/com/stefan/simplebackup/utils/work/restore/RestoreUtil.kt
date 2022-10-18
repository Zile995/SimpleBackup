package com.stefan.simplebackup.utils.work.restore

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.JsonUtil
import com.stefan.simplebackup.utils.root.RootApkManager
import com.stefan.simplebackup.utils.work.WorkResult
import com.stefan.simplebackup.utils.work.WorkUtil
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
                ::installApk
            )
            results.add(result)
        }
        results.toList()
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
        app.updateNotificationData(R.string.restore_progress_dir_info)
        FileUtil.createDirectory(FileUtil.getTempDirPath(app))
        rootApkManager.createTempInstallDir(app)
    }

    private suspend fun unzipData(app: AppData) {
        app.updateNotificationData(R.string.restore_progress_dir_info)
        ZipUtil.extractAllData(app)
        rootApkManager.moveApkFilesToTempDir(app)
    }

    private suspend fun installApk(app: AppData) {
        app.updateNotificationData(R.string.restore_apk_install_info)
        val appManager = AppManager(appContext)
        val doesExists = appManager.doesPackageExists(packageName = app.packageName)
        if (doesExists)
            rootApkManager.uninstallApk(app.packageName)
        rootApkManager.installApk(FileUtil.getTempApkInstallDirPath(app))
    }

    override suspend fun AppData.updateOnSuccess(): WorkResult {
        updateNotificationData(R.string.restore_progress_successful)
        return WorkResult.SUCCESS
    }

    override suspend fun AppData.updateOnFailure(): WorkResult {
        try {
            FileUtil.deleteFile(FileUtil.getTempDirPath(this))
        } catch (e: IOException) {
            Log.w("RestoreUtil", "Failed to delete broken restore files $e")
        } finally {
            setNearestItemInterval()
            updateNotificationData(R.string.restore_progress_failed)
        }
        return WorkResult.ERROR
    }
}

//private suspend fun installApp(context: Context, app: AppData) {
//        withContext(Dispatchers.IO) {
//            val internalStoragePath = (context.getExternalFilesDir(null)!!.absolutePath).run {
//                substring(0, indexOf("Android")).plus(
//                    ROOT
//                )
//            }
//            println(internalStoragePath)
//            val backupDir = app.dataDir
//            val tempDir = LOCAL.plus(backupDir.removePrefix(internalStoragePath))
//            println(tempDir)
//            val packageName = app.packageName
//            val packageDataDir = "$DATA/$packageName"
//            try {
//                with(Installer) {
//                    // TODO: To be fixed.
//                    Shell.su("x=$(echo -e \"$tempDir\") && mkdir -p \"\$x\"").exec()
//                    Shell.su("x=$(echo -e \"$backupDir/${app.packageName}.tar\")" +
//                            " && y=$(echo -e \"$tempDir/\")" +
//                            " && tar -zxf \"\$x\" -C \"\$y\"").exec()
//                    Shell.su("rm -rf $packageDataDir/*").exec()
//                    Shell.su("restorecon -R $packageDataDir").exec()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }