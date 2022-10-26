package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class LocalViewModel : BaseViewModel() {

    init {
        Log.d("ViewModel", "LocalViewModel created")
        refreshBackupList()
        backupFilesObserver.observeBackupFiles()
    }

    fun refreshBackupList() {
        val start = System.currentTimeMillis()
        backupFilesObserver.refreshBackupFileList().invokeOnCompletion {
            viewModelScope.launch {
                if (!spinner.value) return@launch
                val time = System.currentTimeMillis() - start
                Log.d("ViewModel", "Refreshed after $time")
                if (time < 400L) delay(400 - time)
                _spinner.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "LocalViewModel cleared")
    }
}

class LocalViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocalViewModel() as T
        }
        throw IllegalArgumentException("Unable to construct LocalViewModel")
    }
}