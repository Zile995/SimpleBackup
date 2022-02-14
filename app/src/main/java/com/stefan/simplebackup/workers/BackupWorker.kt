package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.utils.backup.BackupUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(Dispatchers.IO) {
                val backupDirPaths = inputData.getStringArray("BACKUP_PATHS")
                backupDirPaths?.let {
                    val backupUtil = BackupUtil(it)
                    backupUtil.backup()
                }
                Log.d("BackupWorker", "Backup successful")
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error + ${e.message}")
            Result.failure()
        }
    }
}