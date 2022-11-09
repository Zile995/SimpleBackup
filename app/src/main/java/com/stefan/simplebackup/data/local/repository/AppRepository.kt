package com.stefan.simplebackup.data.local.repository

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.local.dao.AppDao
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.filterBy
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

typealias RepositoryAction = suspend AppRepository.(CoroutineScope) -> Unit

class AppRepository(private val appDao: AppDao) {

    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    val installedApps
        get() = appDao.getAllApps().filterBy { app ->
            app.isUserApp && !app.isLocal
        }

    suspend fun insertAppData(app: AppData) {
        if (isFavorite(app.packageName) == true) {
            app.isFavorite = true
            insert(app)
        } else {
            insert(app)
        }
    }

    suspend inline fun startRepositoryJob(crossinline repositoryAction: RepositoryAction) =
        coroutineScope {
            launch(ioDispatcher) {
                val time = measureTimeMillis {
                    repositoryAction.invoke(this@AppRepository, this)
                }
                Log.d("AppRepository", "Finished action in $time ms")
            }
        }

    suspend fun removeFromFavorites(packageName: String) = appDao.removeFromFavorites(packageName)

    suspend fun addToFavorites(packageName: String) = appDao.addToFavorites(packageName)

    suspend fun insert(app: AppData) = appDao.insert(app)
    fun findAppsByName(name: String) = appDao.findAppsByName(name)
    suspend fun delete(packageName: String) = appDao.delete(packageName)

    suspend fun getAppData(context: Context, packageName: String): AppData? {
        val appManager = AppManager(context)
        return if (appManager.doesPackageExists(packageName))
            appDao.getData(packageName)
        else {
            delete(packageName)
            null
        }
    }

    private suspend fun isFavorite(packageName: String) = appDao.isFavorite(packageName)
}