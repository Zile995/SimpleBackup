package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.utils.file.BackupFilesObserver
import com.stefan.simplebackup.utils.work.FileUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocalViewModel(appRepository: AppRepository) : BaseViewModel() {

    private var observerJob: Job? = null

    private val backupFilesObserver = BackupFilesObserver(
        rootDirPath = FileUtil.localDirPath,
        scope = viewModelScope,
        appRepository = appRepository
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        Log.d("ViewModel", "LocalViewModel created")
        viewModelScope.launch {
            loadList {
                appRepository.localApps
            }
        }
    }

    suspend fun startObservingBackups() {
        if (observerJob != null) return
        Log.d("ViewModel", "LocalViewModel starting observer")
        spinner.collect { isSpinning ->
            if (!isSpinning) {
                _isRefreshing.value = true
                refreshBackupList().invokeOnCompletion {
                    _isRefreshing.value = false
                }
                observerJob = backupFilesObserver.observeBackupFiles()
            }
        }
    }

    fun stopObservingBackups() {
        Log.d("ViewModel", "LocalViewModel cancelling observer")
        observerJob?.cancel()
        observerJob = null
    }

    fun refreshBackupList() =
        Log.d("ViewModel", "LocalViewModel refreshing backup list").run {
            backupFilesObserver.refreshBackupList()
        }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "LocalViewModel cleared")
    }
}

class LocalViewModelFactory(private val appRepository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocalViewModel(appRepository = appRepository) as T
        }
        throw IllegalArgumentException("Unable to construct LocalViewModel")
    }
}