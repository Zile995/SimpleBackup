package com.stefan.simplebackup.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.filterBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootPackageWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val ioDispatcher = Dispatchers.IO

    override suspend fun doWork(): Result = coroutineScope {
        PreferenceHelper.resetSequenceNumber()
        try {
            if (!PreferenceHelper.isDatabaseCreated) Result.success()
            else {
                val appManager = AppManager(applicationContext)
                val database = AppDatabase.getInstance(applicationContext, this)
                val repository = AppRepository(database.appDao())
                withContext(ioDispatcher) {
                    launch {
                        updateAllPackages(repository, appManager)
                    }
                    deleteUninstalledPackages(repository, appManager)
                }
                Result.success().also {
                    Log.d("BootPackageWorker", "Updated successfully")
                }
            }
        } catch (e: Exception) {
            Log.e("BootPackageWorker", "Error updating database: $e")
            Result.failure()
        }
    }

    private suspend fun updateAllPackages(repository: AppRepository, appManager: AppManager) {
        appManager.apply {
            buildAllData().collect { app ->
                repository.insertAppData(app)
            }
        }
    }

    private suspend fun deleteUninstalledPackages(
        repository: AppRepository, appManager: AppManager
    ) {
        repository.installedApps.filterBy { app ->
            !appManager.doesPackageExists(app.packageName)
        }.take(1).collect { removedApps ->
            removedApps.forEach { app ->
                repository.delete(app.packageName)
            }
        }
    }
}