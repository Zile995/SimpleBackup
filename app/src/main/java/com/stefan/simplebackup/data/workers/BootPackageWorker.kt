package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.utils.PreferenceHelper
import kotlinx.coroutines.*

class BootPackageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainApplication: MainApplication = applicationContext as MainApplication
    private val repository = mainApplication.getRepository
    private val appManager = mainApplication.getAppManager

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                launch {
                    PreferenceHelper.resetSequenceNumber()
                    appManager.apply {
                        dataBuilder().collect { app ->
                            repository.insert(app)
                        }
                    }
                }
                repository.installedApps.collect { databaseList ->
                    databaseList.forEach { app ->
                        if (!appManager.doesPackageExists(app.packageName)) {
                            repository.delete(app.packageName)
                        }
                    }
                }
                Log.d("BootPackageWorker", "Updated successfully")
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BootPackageWorker", "Error updating database: + ${e.message}")
            Result.failure()
        }
    }
}