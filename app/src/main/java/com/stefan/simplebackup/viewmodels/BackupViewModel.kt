package com.stefan.simplebackup.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.utils.backup.BackupWorkerHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel(
    private val app: AppData?,
    application: MainApplication
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    val selectedApp get() = app

    init {
        Log.d("ViewModel", "BackupViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(ioDispatcher) {
            app?.packageName?.let { packageName ->
                val backupWorkerHelper = BackupWorkerHelper(packageName, workManager)
                backupWorkerHelper.startBackupWorker()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "BackupViewModel cleared")
    }
}

class BackupViewModelFactory(
    private val app: AppData?,
    private val application: MainApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(app, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}