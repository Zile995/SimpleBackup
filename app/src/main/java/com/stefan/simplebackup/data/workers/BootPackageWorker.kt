package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.utils.PreferenceHelper
import kotlinx.coroutines.*

class BootPackageWorker(private val appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    private val ioDispatcher = Dispatchers.IO
    private val isDatabaseCreated = PreferenceHelper.isDatabaseCreated

    override suspend fun doWork(): Result = coroutineScope {
        try {
            if (!isDatabaseCreated)
                Result.success()
            else {
                val database = AppDatabase.getInstance(appContext.applicationContext, this)
                val appManager = AppManager(appContext.applicationContext)
                val repository = AppRepository(database.appDao())
                withContext(ioDispatcher) {
                    launch {
                        appManager.apply {
                            dataBuilder().collect { app ->
                                repository.insertAppData(app)
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
                }
                Result.success().also {
                    Log.d("BootPackageWorker", "Updated successfully")
                }
            }
        } catch (e: Throwable) {
            Log.e("BootPackageWorker", "Error updating database: + ${e.message}")
            Result.failure()
        } finally {
            PreferenceHelper.resetSequenceNumber()
        }
    }
}