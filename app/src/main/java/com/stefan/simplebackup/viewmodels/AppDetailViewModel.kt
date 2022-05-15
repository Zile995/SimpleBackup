package com.stefan.simplebackup.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.launch

class AppDetailViewModel(
    private val app: AppData?,
    application: MainApplication
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    val selectedApp get() = app

    init {
        Log.d("ViewModel", "AppDetailViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(ioDispatcher) {
            app?.uid?.let { uid ->
                val workerHelper = WorkerHelper(uid, workManager)
                workerHelper.beginUniqueWork<MainWorker>()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "AppDetailViewModel cleared")
    }
}

class AppDetailViewModelFactory(
    private val app: AppData?,
    private val application: MainApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppDetailViewModel(app, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}