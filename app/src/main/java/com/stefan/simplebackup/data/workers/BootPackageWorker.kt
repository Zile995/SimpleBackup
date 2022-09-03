package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootPackageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    private val database = AppDatabase.getInstance(appContext.applicationContext)
    private val repository = AppRepository(database.appDao())
    private val appManager = AppManager(appContext.applicationContext)

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(ioDispatcher) {
                launch {
                    PreferenceHelper.resetSequenceNumber()
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
                Log.d("BootPackageWorker", "Updated successfully")
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BootPackageWorker", "Error updating database: + ${e.message}")
            Result.failure()
        }
    }
}