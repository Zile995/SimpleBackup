package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
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
    private val appDataType: AppDataType?,
    application: MainApplication
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(application) }

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(Dispatchers.Default) {
            startWorker()
        }
    }

    private fun startWorker() {
        selectionList?.let { appList ->
            appDataType?.let {
                val shouldBackup = it == AppDataType.USER
                val workerHelper = WorkerHelper(appList, workManager)
                workerHelper.beginUniqueWork<MainWorker>(shouldBackup = shouldBackup)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ProgressViewModel", "ProgressViewModel cleared")
    }
}