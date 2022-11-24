package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppInfoManager
import com.stefan.simplebackup.data.manager.AppStorageManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.ui.activities.BaseActivity.Companion.googleDriveService
import com.stefan.simplebackup.utils.extensions.deleteFile
import com.stefan.simplebackup.utils.extensions.createSubFolder
import com.stefan.simplebackup.utils.extensions.fetchOrCreateMainFolder
import com.stefan.simplebackup.utils.extensions.uploadFileToFolder
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.createDirectory
import com.stefan.simplebackup.utils.file.FileUtil.deleteDirectoryFiles
import com.stefan.simplebackup.utils.file.FileUtil.deleteFile
import com.stefan.simplebackup.utils.file.FileUtil.getBackupDirPath
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.FileUtil.moveFiles
import com.stefan.simplebackup.utils.root.RootApkManager
import com.stefan.simplebackup.utils.work.WorkResult
import com.stefan.simplebackup.utils.work.WorkUtil
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.io.File
import java.io.IOException

class BackupUtil(
    private val appContext: Context,
    private val backupItems: Array<String>,
    updateForegroundInfo: ForegroundCallback,
    private val shouldBackupToCloud: Boolean = false
) : WorkUtil(appContext, backupItems, updateForegroundInfo) {

    private var appFolderId: String? = null

    @WorkerThread
    suspend fun backup() = channelFlow {
        var isSkipped: Boolean
        val database = AppDatabase.getInstance(appContext, this)
        val appRepository = AppRepository(database.appDao())
        supervisorScope {
            backupItems.forEach { backupPackageName ->
                isSkipped = false
                val app = appRepository.getAppData(appContext, backupPackageName)
                launch {
                    try {
                        withSuspend(app) {
                            startWork(
                                ::createDirs,
                                ::backupData,
                                ::zipData,
                                ::serializeAppData,
                                ::moveBackup,
                                ::uploadToDrive
                            )
                        }
                    } catch (e: CancellationException) {
                        Log.w("BackupUtil", "Got exception $e")
                        isSkipped = true
                    }
                }.also { itemJob ->
                    send(itemJob)
                }.join()
                if (isSkipped && app != null) onFailure(app)
            }
        }
    }

    private suspend fun createDirs(app: AppData) {
        app.updateProgressData(R.string.backup_progress_dir_info)
        createDirectory(getTempDirPath(app))
        createDirectory(getBackupDirPath(app))
    }

    private suspend fun backupData(app: AppData) {
        app.updateProgressData(R.string.backup_progress_data_info)
        TarUtil.backupData(app)
    }

    private suspend fun zipData(app: AppData) {
        app.updateProgressData(R.string.backup_progress_zip_info)
        ZipUtil.zipAllData(app)
    }

    private suspend fun serializeAppData(app: AppData) {
        app.apply {
            updateProgressData(R.string.backup_progress_saving_application_data)
            setCurrentDate()
            setDataSize(app)
            serialize(getTempDirPath(app = this))
        }
    }

    private suspend inline fun withSuspend(
        app: AppData?,
        crossinline onSuspend: suspend AppData?.() -> Unit
    ) {
        app?.let { RootApkManager.suspendPackage(it.packageName) }
        app.onSuspend()
        app?.let { RootApkManager.unsuspendPackage(it.packageName) }
    }

    private fun setDataSize(app: AppData) {
        if (Shell.isAppGrantedRoot() == false) {
            val appInfoManager = AppInfoManager(appContext.packageManager, 0)
            val appStorageManager = AppStorageManager(appContext)
            val appInfo = appInfoManager.getAppInfo(app.packageName)
            val apkSizeStats = appStorageManager.getApkSizeStats(appInfo)
            app.dataSize = apkSizeStats.dataSize
            app.cacheSize = apkSizeStats.cacheSize
        }
    }

    private suspend fun uploadToDrive(app: AppData) {
        if (shouldBackupToCloud) {
            app.apply {
                updateProgressData(R.string.uploading_to_cloud)
                val appBackupDirPath = getBackupDirPath(app)
                googleDriveService?.let { service ->
                    val jsonFile = FileUtil.getJsonInDir(appBackupDirPath)
                        ?: throw IOException("Upload failed, unable to find json data")
                    val apkZipFile = ZipUtil.getApkZipFile(appBackupDirPath = appBackupDirPath).file
                    val tarZipFile = ZipUtil.getTarZipFile(appBackupDirPath).file

                    try {
                        val parentFolderId =
                            service.fetchOrCreateMainFolder(folderName = appContext.getString(R.string.app_name))

                        appFolderId = service.createSubFolder(
                            parentFolderId = parentFolderId,
                            subFolderName = app.packageName
                        )

                        service.uploadFileToFolder(
                            inputFile = jsonFile,
                            mimeType = "application/json",
                            parentFolderId = appFolderId!!
                        )

                        updateProgressData(R.string.uploading_apk_cloud)
                        service.uploadFileToFolder(
                            inputFile = apkZipFile,
                            mimeType = "application/zip",
                            parentFolderId = appFolderId!!
                        )

                        updateProgressData(R.string.uploading_data_cloud)
                        service.uploadFileToFolder(
                            inputFile = tarZipFile,
                            mimeType = "application/zip",
                            parentFolderId = appFolderId!!
                        )
                    } catch (e: GoogleJsonResponseException) {
                        Log.w("BackupUtil", "Unable to upload file: ${e.details}")
                        throw IOException(e.details.toString())
                    }
                } ?: throw IOException("Upload failed, not signed in!")
            }
        }
    }

    private suspend fun deleteBackupOnDrive() {
        appFolderId?.let {
            googleDriveService?.deleteFile(it)
        }
    }

    private suspend fun moveBackup(app: AppData) {
        val tempDirFile = File(getTempDirPath(app))
        val localDirFile = File(getBackupDirPath(app))
        deleteDirectoryFiles(localDirFile)
        moveFiles(sourceDir = tempDirFile, targetFile = localDirFile)
    }

    override suspend fun onSuccess(app: AppData) {
        app.updateProgressData(R.string.backup_progress_successful, WorkResult.SUCCESS)
    }

    override suspend fun onFailure(app: AppData) {
        super.onFailure(app)
        try {
            Log.w("BackupUtil", "Deleting failed backup.")
            if (shouldBackupToCloud) {
                deleteBackupOnDrive()
            }
            deleteFile(getTempDirPath(app))
        } catch (e: IOException) {
            Log.w("BackupUtil", "Failed to delete backup $e")
        } finally {
            RootApkManager.unsuspendPackage(app.packageName)
            app.updateProgressData(R.string.backup_progress_failed, WorkResult.ERROR)
        }
    }
}