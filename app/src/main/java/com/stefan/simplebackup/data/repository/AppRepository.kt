package com.stefan.simplebackup.data.repository

import com.stefan.simplebackup.data.database.AppDao
import com.stefan.simplebackup.data.model.AppData

class AppRepository(private val appDao: AppDao) {
    val installedApps get() = appDao.getInstalledApps()
    val localApps get() = appDao.getBackupApps()
    val cloudApps get() = appDao.getBackupApps(true)

    suspend fun insert(app: AppData) = appDao.insert(app)

    suspend fun delete(packageName: String) = appDao.delete(packageName)

    suspend fun deleteBackup(packageName: String, selectCloudOnly: Boolean = false) =
        appDao.deleteBackup(packageName, selectCloudOnly)

    suspend fun getAppData(uid: Int) = appDao.getData(uid)

    suspend fun insertOrUpdate(app: AppData) = appDao.insertOrUpdate(app)

    suspend fun getAppData(packageName: String) = appDao.getData(packageName)

    fun doesExist(packageName: String, checkCloudOnly: Boolean = false) = appDao.doesExist(packageName, checkCloudOnly)

    suspend fun getProgressAppData(packageName: String) =
        appDao.getProgressData(packageName)
}