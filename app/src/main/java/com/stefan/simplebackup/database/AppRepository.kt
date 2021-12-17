package com.stefan.simplebackup.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.stefan.simplebackup.data.Application
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    private val _allApps: Flow<MutableList<Application>> = getAppList()
    val getAllApps: LiveData<MutableList<Application>> get() = _allApps.asLiveData()

    suspend fun insert(app: Application) {
        appDao.insert(app)
    }

    private fun getAppList() = appDao.getAppList()

}