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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val app: AppData?,
    application: MainApplication
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    val selectedApp get() = app

    init {
        Log.d("ViewModel", "DetailsViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(Dispatchers.Default) {
            app?.apply {
                val workerHelper = WorkerHelper(uid, workManager)
                workerHelper.beginUniqueWork<MainWorker>()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "DetailsViewModel cleared")
    }
}

class DetailsViewModelFactory(
    private val app: AppData?,
    private val application: MainApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailsViewModel(app, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}