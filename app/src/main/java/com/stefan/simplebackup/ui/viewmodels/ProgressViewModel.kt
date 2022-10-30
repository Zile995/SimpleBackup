package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(
    val appDataType: AppDataType?,
    private val selectionList: Array<String>?,
    private val workManager: WorkManager
) : ViewModel() {

    val numberOfItems get() = selectionList?.size ?: 1
    val progressDataObserver = MainWorker.progressData

    val progressDataList = mutableListOf<ProgressData>()

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(Dispatchers.Default) {
            startWorker()
        }
    }

    fun updateProgressDataList(progressData: ProgressData) {
        if (progressDataList.contains(progressData)) return
        progressDataList.add(progressData)
    }

    private fun startWorker() {
        selectionList?.let { appList ->
            appDataType?.run {
                val workerHelper = WorkerHelper(appList, workManager)
                when (this) {
                    AppDataType.USER -> workerHelper.beginUniqueLocalWork<MainWorker>(shouldBackup = true)
                    AppDataType.LOCAL -> workerHelper.beginUniqueLocalWork<MainWorker>(shouldBackup = false)
                    AppDataType.CLOUD -> workerHelper.beginUniqueCloudWork<MainWorker>()
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
    private val workManager: WorkManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(appDataType, selectionList, workManager) as T
        }
        throw IllegalArgumentException("Unable to construct ProgressViewModel")
    }
}