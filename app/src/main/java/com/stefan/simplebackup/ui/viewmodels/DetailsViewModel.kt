package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val app: AppData?,
    application: MainApplication
) : ViewModel() {

    private val workManager = WorkManager.getInstance(application)
    val selectedApp get() = app

    init {
        Log.d("ViewModel", "DetailsViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(Dispatchers.Default) {
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