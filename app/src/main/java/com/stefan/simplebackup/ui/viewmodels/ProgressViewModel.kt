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
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgressViewModel(
    val appDataType: AppDataType?,
    private val selectionList: Array<String>?,
    application: MainApplication,
) : AndroidViewModel(application) {

    // WorkManager
    private val workManager by lazy { WorkManager.getInstance(application) }

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
    val observableProgressList = progressRepository.progressData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(2000L),
        initialValue = mutableListOf()
    )

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(ioDispatcher) {
            launch {
                startWorker()
                saveProgressPreferences()
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
            appDataType?.let { dataType ->
                val workerHelper = WorkerHelper(appList, workManager)
                when (dataType) {
                    AppDataType.USER -> workerHelper.beginUniqueLocalWork()
                    AppDataType.CLOUD -> workerHelper.beginUniqueCloudBackupWork()
                    AppDataType.LOCAL -> workerHelper.beginUniqueLocalWork(shouldBackup = false)
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
    private val selectionList: Array<String>?,
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