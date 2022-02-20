package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
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

    private lateinit var backupUtil: BackupUtil
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainApplication: DatabaseApplication = applicationContext as DatabaseApplication
    private val repository = mainApplication.getRepository

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                val time = measureTimeMillis {
                    backupUtil = BackupUtil(applicationContext, getApps())
                    backupUtil.backup()
                }
                Log.d("BackupWorker", "Backup successful, completed in: ${time/1000.0} seconds")
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error + ${e.message}")
            Result.failure()
        }
    }

    private suspend fun getApps(): MutableList<AppData> {
        val appList = mutableListOf<AppData>()
        val inputPackageNames = inputData.getStringArray("BACKUP_PACKAGES")
        inputPackageNames?.let { packageNames ->
            packageNames.forEach { packageName ->
                appList.add(repository.getAppByPackageName(packageName))
            }
        }
        return appList
    }
}