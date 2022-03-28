package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.ui.notifications.BackupNotificationBuilder
import com.stefan.simplebackup.utils.backup.BACKUP_ARGUMENT
import com.stefan.simplebackup.utils.backup.BackupUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {

    companion object {
        const val Progress = "BackupProgress"
        const val PROGRESS_MAX = 10000
        private var PROGRESS_CURRENT = 0
    }

    private lateinit var outputData: Data
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val notificationBuilder = BackupNotificationBuilder(appContext, true)
    private val mainApplication: MainApplication = applicationContext as MainApplication
    private val repository = mainApplication.getRepository

    override suspend fun doWork(): Result = coroutineScope {
        try {
            outputData = workDataOf(BACKUP_ARGUMENT to false)
            withContext(ioDispatcher) {
                val time = measureTimeMillis {
                    backup()
                }
                Log.d("BackupWorker", "Backup successful, completed in: ${time / 1000.0} seconds")
                Result.success(outputData)
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error + ${e.message}")
            Result.failure(outputData)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            notificationBuilder.getNotificationId,
            notificationBuilder.createNotification()
        )
    }

    private suspend fun backup() {
        PROGRESS_CURRENT = 0
        val appList = getApps()
        val interval = PROGRESS_MAX / appList.size
        for (i in 0 until appList.size) {
            setForeground(updateForegroundInfo(PROGRESS_CURRENT, appList[i]))
            val backupUtil = BackupUtil(applicationContext, appList[i])
            backupUtil.backup()
            PROGRESS_CURRENT += interval
        }
        outputData = workDataOf(BACKUP_ARGUMENT to true)
    }

    private suspend fun updateForegroundInfo(
        currentProgress: Int,
        app: AppData
    ): ForegroundInfo {
        notificationBuilder.apply {
            setProgress(workDataOf(Progress to currentProgress))
            return ForegroundInfo(
                notificationBuilder.getNotificationId,
                getProgressNotificationBuilder
                    .updateNotificationContent(app)
                    .setProgress(PROGRESS_MAX, currentProgress, false)
                    .build()
            )
        }
    }

    private suspend fun getApps(): MutableList<AppData> {
        val appList = mutableListOf<AppData>()
        val inputPackageNames = inputData.getStringArray(BACKUP_ARGUMENT)
        inputPackageNames?.let { packageNames ->
            packageNames.forEach { packageName ->
                appList.add(repository.getAppByPackageName(packageName))
            }
        }
        return appList
    }
}