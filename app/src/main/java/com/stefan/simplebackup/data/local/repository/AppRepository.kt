package com.stefan.simplebackup.data.local.repository

import android.util.Log
import com.stefan.simplebackup.data.local.dao.AppDao
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.filterBy
import com.stefan.simplebackup.utils.work.FileUtil
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

typealias RepositoryAction = suspend AppRepository.(CoroutineScope) -> Unit

class AppRepository(private val appDao: AppDao) {

    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    val installedApps
        get() = appDao.getAllApps().filterBy { app -> app.isUserApp && !app.isLocal }

    val localApps
        get() = appDao.getAllApps().filterBy { app -> app.isLocal }

    suspend fun insertAppData(app: AppData) {
        if (isFavorite(app) == true) {
            app.isFavorite = true
            insert(app)
        } else {
            insert(app)
        }
    }

    fun findAppsByName(name: String, isLocal: Boolean) = appDao.findAppsByName(name, isLocal)

    suspend inline fun startRepositoryJob(crossinline repositoryAction: RepositoryAction) =
        coroutineScope {
            launch(ioDispatcher) {
                val time = measureTimeMillis {
                    repositoryAction.invoke(this@AppRepository, this)
                }
                Log.d("AppRepository", "Finished action in $time ms")
            }
        }

    suspend fun insert(app: AppData) = appDao.insert(app)
    suspend fun delete(packageName: String) = appDao.delete(packageName)
    suspend fun deleteLocal(packageName: String) = appDao.delete(packageName, true)
    suspend fun addToFavorites(packageName: String) = appDao.addToFavorites(packageName)
    suspend fun removeFromFavorites(packageName: String) = appDao.removeFromFavorites(packageName)

    suspend fun getAppData(appManager: AppManager, packageName: String): AppData? {
        return if (appManager.doesPackageExists(packageName))
            appDao.getAppData(packageName)
        else {
            delete(packageName)
            null
        }
    }

    suspend fun getLocalData(packageName: String): AppData? {
        val app = appDao.getLocalData(packageName)
        val appJsonFile = FileUtil.getJsonFileForApp(app)
        return if (appJsonFile?.exists() == true) {
            app
        } else {
            deleteLocal(packageName)
            null
        }
    }

    private suspend fun isFavorite(app: AppData) = appDao.isFavorite(app.packageName)
}