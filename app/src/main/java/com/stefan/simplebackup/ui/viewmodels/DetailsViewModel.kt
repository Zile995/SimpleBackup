package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
    val app: AppData?,
    application: MainApplication
) : ViewModel() {

    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val appRepository by lazy {
        AppRepository(AppDatabase.getInstance(application).appDao())
    }
    private val workManager by lazy { WorkManager.getInstance(application) }

    private var _archNames = MutableStateFlow<List<String>?>(null)
    val archNames get() = _archNames.asStateFlow()

    init {
        Log.d("ViewModel", "DetailsViewModel created")
        getApkArchitectures()
    }

    private fun getApkArchitectures() {
        viewModelScope.launch(ioDispatcher) {
            app?.apply {
                _archNames.value = ZipUtil.getAppAbiList(this)
            }
        }
    }

    fun addAppToFavorites() {
        viewModelScope.launch {
            appRepository.startRepositoryJob {
                app?.apply {
                    addToFavorites(packageName)
                }
            }
        }
    }

    fun removeAppFromFavorites() {
        viewModelScope.launch {
            appRepository.startRepositoryJob {
                app?.apply {
                    removeFromFavorites(packageName)
                }
            }
        }
    }

    fun createLocalBackup() {
        viewModelScope.launch(defaultDispatcher) {
            app?.apply {
                val workerHelper = WorkerHelper(packageName, workManager)
                workerHelper.beginUniqueWork<MainWorker>()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "DetailsViewModel cleared")
    }
}