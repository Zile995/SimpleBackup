package com.stefan.simplebackup.data.local.repository

import com.stefan.simplebackup.data.local.database.AppDao
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    /**
     * - Filter the given flow list
     */
    private inline fun <T> Flow<List<T>>.filterBy(crossinline predicate: (T) -> Boolean) = run {
        map { list ->
            list.filter(predicate)
        }
    }

    suspend fun insertAppData(app: AppData) {
        if (isFavorite(app.packageName) == true && !app.isLocal) {
            app.favorite = true
            insert(app)
        } else {
            insert(app)
        }
    }

    suspend fun changeFavorites(uid: Int) =
        appDao.updateFavorite(uid)

    suspend fun isFavorite(packageName: String) = appDao.isFavorite(packageName)

    suspend fun insert(app: AppData) = appDao.insert(app)
    suspend fun delete(packageName: String) = appDao.delete(packageName)

    suspend fun getAppData(uid: Int) = appDao.getData(uid)
    suspend fun getAppData(packageName: String) = appDao.getData(packageName)

    fun doesAppDataExist(packageName: String) = appDao.doesAppDataExist(packageName)
}