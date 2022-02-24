package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.database.DatabaseApplication
import kotlinx.coroutines.*

class BootPackageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mainApplication: DatabaseApplication = applicationContext as DatabaseApplication
    private val repository = mainApplication.getRepository
    private val appManager = mainApplication.getAppManager

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                launch {
                    appManager.getApplicationList().collect { newApp ->
                        repository.insert(newApp)
                    }
                }
                repository.getAllApps.collect { databaseList ->
                    databaseList.forEach { app ->
                        if (!appManager.doesPackageExists(app.getPackageName())) {
                            repository.delete(app.getPackageName())
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