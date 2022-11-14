package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.ProgressRepository
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(
    val appDataType: AppDataType?,
    private val selectionList: Array<String>?,
    application: MainApplication,
) : AndroidViewModel(application) {

    // WorkManager
    private val workManager = WorkManager.getInstance(application)

    // Dispatchers
    private val ioDispatcher = Dispatchers.IO

    // Progress values
    val numberOfItems by lazy {
        if (selectionList.isNullOrEmpty())
            PreferenceHelper.numOfWorkItems
        else
            selectionList.size
    }

    // Progress repository
    private val progressRepository by lazy {
        val database = AppDatabase.getInstance(application, application.applicationScope)
        ProgressRepository(database.progressDao())
    }

    // Observable list
    private val _observableProgressList = MutableStateFlow(mutableListOf<ProgressData>())
    val observableProgressList get() = _observableProgressList.asStateFlow()

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(ioDispatcher) {
            launch {
                startWorker()
                saveProgressPreferences()
            }
            launch {
                progressRepository.progressData.collect {
                    _observableProgressList.value = it
                }
            }
        }
    }

    fun clearProgressData() {
        val mainApplication = getApplication<MainApplication>()
        mainApplication
            .applicationScope
            .launch(ioDispatcher) {
                workManager.pruneWork()
                progressRepository.clear()
                PreferenceHelper.removeProgressData()
            }
    }

    private suspend fun saveProgressPreferences() {
        if (!PreferenceHelper.hasSavedProgressData()) {
            PreferenceHelper.saveProgressType(appDataType ?: AppDataType.USER)
            PreferenceHelper.saveNumOfWorkItems(selectionList?.size ?: 1)
        }
    }

    private fun startWorker() {
        selectionList?.let { appList ->
            appDataType?.run {
                val workManager =
                    WorkManager.getInstance(getApplication<MainApplication>().applicationContext)
                val workerHelper = WorkerHelper(appList, workManager)
                when (this) {
                    AppDataType.USER -> workerHelper.beginUniqueLocalWork(shouldBackup = true)
                    AppDataType.LOCAL -> workerHelper.beginUniqueLocalWork(shouldBackup = false)
                    AppDataType.CLOUD -> workerHelper.beginUniqueCloudWork()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ProgressViewModel", "ProgressViewModel cleared")
    }
}

class ProgressViewModelFactory(
    private val appDataType: AppDataType?,
    private val selectionList: Array<String>?
) : ViewModelProvider.AndroidViewModelFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(
                appDataType,
                selectionList,
                extras[APPLICATION_KEY] as MainApplication
            ) as T
        }
        throw IllegalArgumentException("Unable to construct ProgressViewModel")
    }
}