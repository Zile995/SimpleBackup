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
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel(
    val appDataType: AppDataType?,
    private val selectionList: Array<String>?,
    application: MainApplication,
) : AndroidViewModel(application) {

    // Progress values
    val numberOfItems get() = selectionList?.size ?: 1
    val progressDataObserver = MainWorker.progressData
    val progressDataList = mutableListOf<ProgressData>()

    // Progress repository
    private val progressRepository by lazy {
        val database = AppDatabase.getInstance(application, application.applicationScope)
        ProgressRepository(database.progressDao())
    }
    private val workManager = WorkManager.getInstance(application)

    private val ioDispatcher = Dispatchers.IO
    private val defaultDispatcher = Dispatchers.Default

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(defaultDispatcher) {
            startWorker()
        }
    }

    fun updateProgressDataList(progressData: ProgressData) {
        if (progressDataList.contains(progressData)) return
        progressDataList.add(progressData)
    }

    fun clearProgressData() {
        val mainApplication = getApplication<MainApplication>()
        mainApplication
            .applicationScope.launch(ioDispatcher) {
                MainWorker.clearProgressData()
                workManager.pruneWork()
                progressRepository.clear()
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