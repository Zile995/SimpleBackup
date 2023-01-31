package com.stefan.simplebackup.utils.work

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.Drive
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.manager.AppStorageManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.root.RootApkManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.apache.http.entity.ContentType
import java.io.File
import java.io.IOException

class BackupUtil(
    private val appContext: Context,
    private val backupItems: Array<String>,
    updateForegroundInfo: ForegroundCallback,
    private val shouldBackupToCloud: Boolean = false
) : WorkUtil(appContext, backupItems, updateForegroundInfo) {

    private val appManager = AppManager(appContext)

    private val driveService: Drive? by lazy {
        appContext.getDriveService(googleAccount = appContext.getLastSignedInAccount())
    }

    @WorkerThread
    suspend fun backup() = channelFlow {
        var isSkipped: Boolean
        val database = AppDatabase.getInstance(appContext, this)
        val appRepository = AppRepository(database.appDao())
        supervisorScope {
            backupItems.forEach { backupPackageName ->
                isSkipped = false
                val app = appRepository.getAppData(appManager, backupPackageName)
                launch {
                    try {
                        withSuspend(app) {
                            startWork(
                                ::createDirs,
                                ::backupData,
                                ::zipData,
                                ::serializeAppData,
                                ::uploadToDrive,
                                ::moveBackup
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
        FileUtil.createDirectory(tempItemDirPath)
        FileUtil.createDirectory(backupItemDirPath)
    }

    private suspend fun backupData(app: AppData) {
        app.updateProgressData(R.string.backup_progress_data_info)
        TarUtil.backupData(app)
    }

    private suspend fun zipData(app: AppData) {
        app.updateProgressData(R.string.backup_progress_zip_info)
        isZippingData = true
        ZipUtil.zipAllData(app, tempItemDirPath)
        isZippingData = false
    }

    private suspend fun serializeAppData(app: AppData) {
        app.apply {
            updateProgressData(R.string.backup_progress_saving_application_data)
            setCurrentDate()
            setDataSize(app)
            serialize(destinationPath = tempItemDirPath)
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
            val appInfoManager = appManager.appInfoManager
            val appStorageManager = AppStorageManager(appContext)
            val appInfo = appInfoManager.getAppInfo(app.packageName)
            val apkSizeStats = appStorageManager.getApkSizeStats(appInfo)
            app.dataSize = apkSizeStats.dataSize
            app.cacheSize = apkSizeStats.cacheSize
        }
    }

    private suspend fun uploadToDrive(app: AppData) {
        if (shouldBackupToCloud) {
            app.updateProgressData(R.string.uploading_to_cloud)
            driveService?.let { service ->
                val jsonFile = FileUtil.getJson(tempItemDirPath)
                    ?: throw IOException("Upload failed, unable to find json data")
                val apkZipFile = ZipUtil.getApkZipFile(appBackupDirPath = tempItemDirPath).file
                val tarZipFile = ZipUtil.getTarZipFile(tempItemDirPath).file

                try {
                    // Create main folder
                    val mainFolderId =
                        service.fetchOrCreateMainFolder(folderName = appContext.getString(R.string.app_name))

                    // Create temp folder
                    val tempFolderId = service.createFolder(
                        folderName = TEMP_DIR_NAME,
                        parentId = mainFolderId
                    )

                    // Create package folder in temp folder
                    val packageFolderId = service.createFolder(
                        folderName = TEMP_DIR_NAME + "_" + app.packageName,
                        parentId = tempFolderId
                    )

                    app.updateProgressData(R.string.uploading_apk_cloud)
                    service.uploadFileToFolder(
                        inputFile = apkZipFile,
                        mimeType = APPLICATION_ZIP.mimeType,
                        parentFolderId = packageFolderId
                    )

                    app.updateProgressData(R.string.uploading_data_cloud)
                    service.uploadFileToFolder(
                        inputFile = tarZipFile,
                        mimeType = APPLICATION_ZIP.mimeType,
                        parentFolderId = packageFolderId
                    )

                    service.uploadFileToFolder(
                        inputFile = jsonFile,
                        mimeType = ContentType.APPLICATION_JSON.mimeType,
                        parentFolderId = packageFolderId
                    )

                    // Delete old backup
                    service.deleteFile(app.packageName)

                    // Move new backup to main folder
                    service.moveFile(packageFolderId, mainFolderId)

                    // Rename backup folder
                    service.renameFile(fileId = packageFolderId, newFileName = app.packageName)
                } catch (e: GoogleJsonResponseException) {
                    Log.w("BackupUtil", "Unable to upload file: ${e.details}")
                    throw IOException(e.details.toString())
                }
            } ?: throw IOException("Upload failed, not signed in!")
        }
    }

    private suspend fun deleteBackupOnDrive() {
        if (shouldBackupToCloud) {
            driveService?.deleteFile(fileName = TEMP_DIR_NAME)
        }
    }

    private suspend fun moveBackup(app: AppData) {
        if (shouldBackupToCloud) {
            FileUtil.deleteFile(FileUtil.getTempDirPath(app))
            return
        }
        val tempDirFile = File(tempItemDirPath)
        val localDirFile = File(backupItemDirPath)
        FileUtil.deleteFile(localDirFile.absolutePath)
        FileUtil.moveFiles(sourceDir = tempDirFile, targetFile = localDirFile)
    }

    override suspend fun onSuccess(app: AppData) {
        app.updateProgressData(R.string.backup_progress_successful, WorkResult.SUCCESS)
    }

    override suspend fun onFailure(app: AppData) {
        super.onFailure(app)
        try {
            Log.w("BackupUtil", "Deleting failed backup.")
            deleteBackupOnDrive()
            FileUtil.deleteFile(path = tempItemDirPath)
        } catch (e: IOException) {
            Log.w("BackupUtil", "Failed to delete backup $e")
        } finally {
            RootApkManager.unsuspendPackage(app.packageName)
            app.updateProgressData(R.string.backup_progress_failed, WorkResult.ERROR)
        }
    }

    companion object {
        var isZippingData = false
            private set
    }
}