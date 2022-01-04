package com.stefan.simplebackup.workers

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stefan.simplebackup.data.AppBuilder
import com.stefan.simplebackup.database.AppDatabase
import com.stefan.simplebackup.database.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class BootPackageWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(
    appContext,
    params
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val appBuilder = AppBuilder(appContext)
    private val database = AppDatabase.getDbInstance(applicationContext, scope, appBuilder)
    private val repository = AppRepository(database.appDao())

    override suspend fun doWork(): Result = coroutineScope {
        try {
            withContext(Dispatchers.IO) {
                val newList = appBuilder.getApplicationList().toMutableList()
                newList.forEach {
                    repository.insert(it)
                }
                repository.getAllApps.asFlow().collect {
                    it.forEach { app ->
                        if (!appBuilder.doesPackageExists(app.getPackageName())) {
                            repository.delete(app.getPackageName())
                        }
                    }
                }
                Log.d("BootPackageWorker", "Updated successfully")
                Result.success()
            }
        } catch (e: Throwable) {
            Log.e("BootPackageWorker", "Error updating database")
            Result.failure()
        }
    }
}