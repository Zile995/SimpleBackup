package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.MainApplication.Companion.backupDirPath
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.extensions.launchWithLogging
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LocalViewModel(application: MainApplication) : BaseViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val repository by lazy { AppRepository(application.database.appDao()) }
    //private val fileWatcher = File(mainBackupDirPath).asFileWatcher()

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    val localApps = repository.localApps.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(4_000L),
            mutableListOf()
        )

    init {
        Log.d("ViewModel", "LocalViewModel created")
        viewModelScope.launchWithLogging(CoroutineName("LoadLocalList")) {
            launch {
                delay(400)
                _spinner.emit(false)
            }
        }
    }

    suspend fun startPackagePolling() {
        withContext(Dispatchers.Default) {
            launch {
                while (true) {
                    findJsonFiles(backupDirPath).collect { jsonFile ->
                        val app = deserializeApp(jsonFile)
                        app?.let {
                            repository.insertOrUpdate(it)
                        }
                    }
                    delay(1_500)
                }
            }
        }
    }

    fun startRestoreWorker(uid: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val workerHelper = WorkerHelper(uid, workManager)
            workerHelper.beginUniqueWork<MainWorker>(shouldBackup = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "LocalViewModel cleared")
    }
}

class LocalViewModelFactory(private val application: MainApplication) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(LocalViewModel::class.java)) {
            return LocalViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}