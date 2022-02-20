package com.stefan.simplebackup.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.backup.BackupWorkerHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel(
    private val app: AppData?,
    private val application: DatabaseApplication
) : AndroidViewModel(application) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val selectedApp get() = app

    init {
        Log.d("ViewModel", "BackupViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(ioDispatcher) {
            app?.let { backupApp ->
                val backupWorkerHelper = BackupWorkerHelper(backupApp, application)
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
    private val application: DatabaseApplication
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