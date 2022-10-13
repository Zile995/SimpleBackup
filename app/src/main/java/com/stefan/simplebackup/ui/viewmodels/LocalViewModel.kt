package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class LocalViewModel(
    application: MainApplication
) : BaseViewModel() {

    private val workManager = WorkManager.getInstance(application)

    init {
        Log.d("ViewModel", "LocalViewModel created")
        refreshBackupList()
        backupFilesObserver.observeBackupFiles()
    }

    fun refreshBackupList() {
        viewModelScope.launch {
            val time = measureTimeMillis {
                backupFilesObserver.refreshBackupFileList()
            }
            Log.d("ViewModel", "Refreshed after $time")
            if (time < 400L && spinner.value) delay(400 - time)
            _spinner.value = false
        }
    }

    fun startRestoreWorker(packageName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val workerHelper = WorkerHelper(packageName = packageName, workManager)
            workerHelper.beginUniqueWork<MainWorker>(shouldBackup = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "LocalViewModel cleared")
    }
}

class LocalViewModelFactory(
    private val application: MainApplication,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocalViewModel(application) as T
        }
        throw IllegalArgumentException("Unable to construct LocalViewModel")
    }
}