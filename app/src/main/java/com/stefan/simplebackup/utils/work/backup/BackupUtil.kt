package com.stefan.simplebackup.utils.work.backup

import android.content.Context
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.work.archive.TarUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(
    appContext: Context,
    private val backupItems: IntArray
) : FileHelper {

    private val notificationData = NotificationData()
    private val repository = (appContext as MainApplication).getRepository

    private var currentProgress = 0
    private val updateProgress: () -> Unit = {
        currentProgress += (PROGRESS_MAX / backupItems.size) / 3
    }

    suspend fun backup() = flow {
        backupItems.forEach { item ->
            repository.getAppData(item).also { app ->
                PreferenceHelper.savePackageName(app.packageName)
                emitProgress(
                    app,
                    ::createDirs,
                    ::backupData,
                    ::zipData
                )
                outputAppInfo(app)
            }
        }
    }

    private suspend fun createDirs(app: AppData) = flow {
        emit(currentProgress to "Creating app dirs")
        createMainDir()
        createAppBackupDir(getBackupDirPath(app))
        updateProgress()
    }

    private suspend fun backupData(app: AppData) = flow {
        emit(currentProgress to "Backing up app data")
        TarUtil.backupData(app)
        updateProgress()
    }

    private suspend fun zipData(app: AppData) = flow {
        emit(currentProgress to "Backing up apk's and zipping all data")
        ZipUtil.zipAllData(app)
        updateProgress()
        emit(currentProgress to "Successfully backed up ${app.name}")
    }

    private suspend fun outputAppInfo(app: AppData) {
        setBackupTime(app)
        serializeApp(app)
    }

    private suspend inline fun FlowCollector<NotificationData>.emitProgress(
        workData: AppData,
        vararg actions: suspend (workData: AppData) -> Flow<Pair<Int, String?>>
    ) {
        actions.forEach { action ->
            emit(notificationData.apply {
                action(workData).collect { status ->
                    name = workData.name
                    image = workData.bitmap
                    progress = status.first
                    status.second?.let {
                        text = it
                    }
                }
            })
        }
    }
}