package com.stefan.simplebackup.data.repository

import com.stefan.simplebackup.data.database.AppDao
import com.stefan.simplebackup.data.model.AppData

class AppRepository(private val appDao: AppDao) {
    val installedApps get() = appDao.getAllApps()
    val localApps get() = appDao.getLocalList()

    suspend fun insert(app: AppData) = appDao.insert(app)

    suspend fun delete(packageName: String) = appDao.delete(packageName)

    suspend fun getAppData(uid: Int) = appDao.getData(uid)

    suspend fun getAppData(packageName: String) = appDao.getData(packageName)

    fun doesExist(packageName: String) = appDao.doesExist(packageName)

    suspend fun getProgressAppData(packageName: String) =
        appDao.getProgressAppData(packageName)
}