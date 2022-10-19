package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val selectionList: Array<String>?,
    val appDataType: AppDataType?,
    application: MainApplication
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(application) }

    val numberOfItems get() = selectionList?.size ?: 1

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(Dispatchers.Default) {
            startWorker()
        }
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
    private val selectionList: Array<String>?,
    private val appDataType: AppDataType?,
    private val application: MainApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(selectionList, appDataType, application) as T
        }
        throw IllegalArgumentException("Unable to construct ProgressViewModel")
    }
}