package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.*
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppInfoManager
import com.stefan.simplebackup.data.manager.AppStorageManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.ui.activities.BaseActivity.Companion.googleDriveService
import com.stefan.simplebackup.utils.file.FileUtil
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

    @WorkerThread
    suspend fun backup() = channelFlow {
        var isSkipped: Boolean
        val database = AppDatabase.getInstance(appContext, this)
        val appRepository = AppRepository(database.appDao())
        backupItems.forEach { backupPackageName ->
            isSkipped = false
            val app = appRepository.getAppData(appContext, backupPackageName)
            supervisorScope {
                launch {
                    try {
                        withSuspend(app) {
                            startWork(
                                ::createDirs,
                                ::backupData,
                                ::zipData,
                                ::serializeAppData,
                                ::moveBackup,
                                ::uploadToCloud
                            )
                        }
                    } catch (e: CancellationException) {
                        Log.w("BackupUtil", "Got exception $e")
                        isSkipped = true
                    }
                }.also { perItemJob ->
                    send(perItemJob)
                }.join()
                launch {
                    if (isSkipped && app != null) onFailure(app)
                }
            }
        }
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
            setDataSize(app)
            serializeApp(getTempDirPath(app = this))
        }
    }

    private suspend inline fun withSuspend(
        app: AppData?,
        crossinline doWhileSuspended: suspend AppData?.() -> Unit
    ) {
        app?.let { suspendApp(it) }
        app.doWhileSuspended()
        app?.let { unsuspendApp(it) }
    }

    private fun suspendApp(app: AppData) {
        Shell.cmd("cmd package suspend ${app.packageName}").exec()
    }

    private fun unsuspendApp(app: AppData) {
        Shell.cmd("cmd package unsuspend ${app.packageName}").exec()
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

    private suspend fun uploadToCloud(app: AppData) {
        if (shouldBackupToCloud) {
            app.apply {
                updateNotificationData(R.string.upload_to_cloud)
                val appBackupDirPath = getBackupDirPath(app)
                googleDriveService?.let { service ->
                    val jsonFile = FileUtil.getJsonInDir(appBackupDirPath)
                        ?: throw IOException("Upload failed, unable to find json data")
                    val apkZipFile =
                        ZipUtil.getApkZipFile(appBackupDirPath = appBackupDirPath).file
                            ?: throw IOException("Upload failed, unable to find apk zip")
                    val tarZipFile = ZipUtil.getTarZipFile(appBackupDirPath).file
                        ?: throw IOException("Upload failed, unable to find tar zip")

                    val jsonFileMetaData = File().apply { name = jsonFile.name }
                    val apkZipFileMetaData = File().apply { name = apkZipFile.name }
                    val tarZipFileMetaData = File().apply { name = tarZipFile.name }

                    val jsonMediaContent = FileContent("application/json", jsonFile)
                    val apkZipMediaContent = FileContent("application/zip", apkZipFile)
                    val tarZipMediaContent = FileContent("application/zip", tarZipFile)
                    try {
                        val parentFolderId = createAppDirOnDrive(service, app)

                        uploadFileToDriveFolder(
                            service,
                            jsonFileMetaData,
                            jsonMediaContent,
                            parentFolderId
                        )
                        updateNotificationData(R.string.uploading_apk_cloud)
                        uploadFileToDriveFolder(
                            service,
                            apkZipFileMetaData,
                            apkZipMediaContent,
                            parentFolderId
                        )
                        updateNotificationData(R.string.uploading_data_cloud)
                        uploadFileToDriveFolder(
                            service,
                            tarZipFileMetaData,
                            tarZipMediaContent,
                            parentFolderId
                        )

                    } catch (e: GoogleJsonResponseException) {
                        Log.w("BackupUtil", "Unable to upload file: ${e.details}")
                        throw IOException(e.details.toString())
                    }
                } ?: throw IOException("Upload failed, not signed in!")
            }
        }
    }

    private fun createAppDirOnDrive(service: Drive, app: AppData): String {
        val fileMetadata: com.google.api.services.drive.model.File = File()
        fileMetadata.name = app.packageName
        fileMetadata.mimeType = "application/vnd.google-apps.folder"
        return createFileOnDrive(service, fileMetadata)
    }

    private fun uploadFileToDriveFolder(
        service: Drive,
        content: com.google.api.services.drive.model.File,
        mediaContent: AbstractInputStreamContent,
        parentFolderId: String
    ): String {
        content.parents = mutableListOf(parentFolderId)
        val file = service.files().create(content, mediaContent)
            .setFields("id, parents")
            .execute()
        return file.id
    }

    private fun createFileOnDrive(
        service: Drive,
        content: com.google.api.services.drive.model.File
    ): String {
        val file = service.files().create(content)
            .setFields("id")
            .execute()
        return file.id
    }

    private suspend fun moveBackup(app: AppData) {
        val tempDirFile = File(getTempDirPath(app))
        val localDirFile = File(getBackupDirPath(app))
        deleteDirectoryFiles(localDirFile)
        moveFiles(sourceDir = tempDirFile, targetFile = localDirFile)
    }

    override suspend fun onSuccess(app: AppData) {
        app.updateNotificationData(R.string.backup_progress_successful, WorkResult.SUCCESS)
    }

    override suspend fun onFailure(app: AppData) {
        super.onFailure(app)
        try {
            Log.w("BackupUtil", "Deleting failed backup.")
            deleteFile(getTempDirPath(app))
        } catch (e: IOException) {
            Log.w("BackupUtil", "Failed to delete broken backup $e")
        } finally {
            unsuspendApp(app)
            app.updateNotificationData(R.string.backup_progress_failed, WorkResult.ERROR)
        }
    }
}