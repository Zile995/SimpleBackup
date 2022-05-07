package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.stefan.simplebackup.ui.notifications.NotificationBuilder
import com.stefan.simplebackup.utils.backup.ARGUMENT
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

const val RESTORE_PROGRESS = "RestoreProgress"

class RestoreWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {

    private lateinit var outputData: Data
    private val notificationBuilder = NotificationBuilder(appContext, true)
    private val packageNames: Array<String>?
        get() = inputData.getStringArray(ARGUMENT)

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                val time = measureTimeMillis {
                    restore()
                }
                Log.d("RestoreWorker", "Backup successful, completed in: ${time / 1000.0} seconds")
                Result.success(outputData).also {
                    // TODO: Send broadcast notification
                }
            }
        } catch (e: Throwable) {
            Log.e("BackupWorker", "Backup error + ${e.message}")
            Result.failure(outputData)
        }
    }

    private suspend fun restore() {

    }
}