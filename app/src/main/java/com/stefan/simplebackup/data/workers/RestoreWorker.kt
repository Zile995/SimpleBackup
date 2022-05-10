package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.stefan.simplebackup.ui.notifications.NotificationBuilder
import com.stefan.simplebackup.ui.notifications.NotificationHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

const val RESTORE_PROGRESS = "RestoreProgress"

class RestoreWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
), NotificationHelper by NotificationBuilder(appContext) {

    private lateinit var outputData: Data
    private val packageNames: Array<String>?
        get() = inputData.getStringArray(ARGUMENT)

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                outputData = workDataOf(ARGUMENT to false)
                val time = measureTimeMillis {
                    restore()
                }
                Log.d("RestoreWorker", "Restore successful, completed in: ${time / 1000.0} seconds")
                Result.success(outputData).also {
                    // TODO: Send broadcast notification
                    delay(1_000)
                    packageNames?.apply {
                        applicationContext.sendNotificationBroadcast(
                            notification = getFinishedNotification(numberOfPackages = size, isBackup = false)
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("RestoreWorker", "Restore error: ${e.message}")
            Result.failure(outputData)
        }
    }

    private suspend fun restore() {

    }
}