package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.database.AppDatabase
import com.stefan.simplebackup.database.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class BootPackageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val appManager = AppManager(appContext)
    private val database = AppDatabase.getDbInstance(applicationContext, scope, appManager)
    private val repository = AppRepository(database.appDao())

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(Dispatchers.IO) {
                launch {
                    val newList = appManager.getApplicationList()
                    newList.forEach { newApp ->
                        repository.insert(newApp)
                    }
                }
                launch {
                    repository.getAllApps.asFlow().collect { databaseList ->
                        databaseList.forEach { app ->
                            if (!appManager.doesPackageExists(app.getPackageName())) {
                                repository.delete(app.getPackageName())
                            }
                        }
                    }
                }
                appManager.updateSequenceNumber()
                Log.d("BootPackageWorker", "Updated successfully")
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BootPackageWorker", "Error updating database: + ${e.message}")
            Result.failure()
        }
    }
}