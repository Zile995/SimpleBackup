package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import com.stefan.simplebackup.utils.file.Kind
import com.stefan.simplebackup.utils.file.asFileWatcher
import com.stefan.simplebackup.utils.main.launchWithLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RestoreViewModel(application: MainApplication) : BaseAndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val repository = application.getRepository
    private val fileWatcher = File(mainBackupDirPath).asFileWatcher()

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    val localApps by lazy {
        repository.localApps.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(4_000L),
            mutableListOf()
        )
    }

    init {
        Log.d("ViewModel", "RestoreViewModel created")
        viewModelScope.launchWithLogging {
            launch {
                localApps
                delay(350)
                _spinner.emit(false)
            }
            fileWatcher.processEvents().collect { fileEvent ->

            }

        }
    }

    suspend fun startPackagePolling() {
        withContext(Dispatchers.Default) {
            launch {
                while (true) {
                    findJsonFiles(mainBackupDirPath).collect { jsonFile ->
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

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "RestoreViewModel cleared")
    }
}

class RestoreViewModelFactory(private val application: MainApplication) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(RestoreViewModel::class.java)) {
            return RestoreViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}