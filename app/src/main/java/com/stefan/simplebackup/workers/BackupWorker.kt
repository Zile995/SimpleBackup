package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.database.AppDatabase
import com.stefan.simplebackup.database.AppRepository
import com.stefan.simplebackup.utils.backup.BackupUtil
import com.stefan.simplebackup.utils.backup.ROOT
import kotlinx.coroutines.*

class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {

    private lateinit var backupUtil: BackupUtil

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob())
    private val appManager = AppManager(appContext)
    private val database = AppDatabase.getDbInstance(applicationContext, scope, appManager)
    private val repository = AppRepository(database.appDao())

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                backupUtil = BackupUtil(applicationContext, getApps())
                backupUtil.backup()
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