package com.stefan.simplebackup.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    private val _allApps: Flow<MutableList<AppData>> = getAppList()
    val getAllApps: LiveData<MutableList<AppData>> get() = _allApps.asLiveData()

    private fun getAppList() = appDao.getAppList()

    suspend fun insert(app: AppData) {
        appDao.insert(app)
    }

    suspend fun delete(packageName: String) {
        appDao.delete(packageName)
    }

    suspend fun getAppByPackageName(packageName: String): AppData {
        return appDao.getAppByPackageName(packageName)
    }
}