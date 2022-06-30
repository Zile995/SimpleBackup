package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val selectionList: IntArray?,
    application: MainApplication
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(Dispatchers.Default) {
            startWorker()
        }
    }

    private fun startWorker() {
        selectionList?.let { appList ->
            val workerHelper = WorkerHelper(appList, workManager)
            workerHelper.beginUniqueWork<MainWorker>()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ProgressViewModel", "ProgressViewModel cleared")
    }
}