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
        return@coroutineScope try {
            withContext(Dispatchers.IO) {
                val backupDirPaths = inputData.getStringArray("BACKUP_PATHS")
                if (backupDirPaths != null) {
                    val backupUtil = BackupUtil(backupDirPaths)
                    backupUtil.backup()
                }
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error + ${e.message}")
            Result.failure()
        }
    }
}