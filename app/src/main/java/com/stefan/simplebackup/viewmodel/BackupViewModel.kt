package com.stefan.simplebackup.viewmodel

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.backup.BackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel(
    private val app: AppData?,
    private val application: DatabaseApplication
) : AndroidViewModel(application) {

    val selectedApp get() = app

    init {
        Log.d("ViewModel", "BackupViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            app?.let { backupApp ->
                val backupHelper = BackupHelper(backupApp, application)
                backupHelper.localBackup()
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