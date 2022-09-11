package com.stefan.simplebackup.data.local.repository

import com.stefan.simplebackup.data.local.database.AppDao
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.filterBy

typealias RepositoryPackageNameAction = suspend AppRepository.(String) -> Unit

class AppRepository(private val appDao: AppDao) {
    val installedApps
        get() = appDao.getAllApps().filterBy { app ->
            app.isUserApp && !app.isLocal
        }
    val localApps
        get() = appDao.getAllApps().filterBy { app ->
            app.isLocal
        }
    val cloudApps
        get() = appDao.getAllApps().filterBy { app ->
            app.isCloud
        }

    suspend fun insertAppData(app: AppData) {
        if (isFavorite(app.packageName) == true) {
            app.favorite = true
            insert(app)
        } else {
            insert(app)
        }
    }

    suspend fun insert(app: AppData) = appDao.insert(app)
    fun findAppsByName(name: String) = appDao.findAppsByName(name)
    suspend fun delete(packageName: String) = appDao.delete(packageName)
    suspend fun getAppData(packageName: String) = appDao.getData(packageName)
    suspend fun isFavorite(packageName: String) = appDao.isFavorite(packageName)
    suspend fun addToFavorites(packageName: String) = appDao.addToFavorites(packageName)
    suspend fun removeFromFavorites(packageName: String) = appDao.removeFromFavorites(packageName)
}