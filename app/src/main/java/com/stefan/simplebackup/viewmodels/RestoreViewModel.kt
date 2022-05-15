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
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.file.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RestoreViewModel(application: MainApplication) : AndroidViewModel(application) {

    //    private val appManager = application.getAppManager
    private val workManager by lazy { WorkManager.getInstance(application) }
    private val repository = application.getRepository

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    private var _localApps = MutableStateFlow(mutableListOf<AppData>())
    val localApps: StateFlow<MutableList<AppData>> get() = _localApps

    // Selection properties
    val selectionList = mutableListOf<Int>()

    private var _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> get() = _isSelected
    val setSelectionMode = { isSelected: Boolean -> _isSelected.value = isSelected }

    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = this::state.isInitialized

    init {
        Log.d("ViewModel", "RestoreViewModel created")
        viewModelScope.launch {
            launch {
                repository.localApps.collect { localApps ->
                    _localApps.value = localApps
                    delay(350)
                    _spinner.emit(false)
                }
            }
            startPackagePolling()
        }
    }

    private suspend fun startPackagePolling() {
        withContext(Dispatchers.Default) {
            while (true) {
                val dir = File(mainBackupDirPath)
                dir.listFiles()?.forEach { appDirList ->
                    appDirList.listFiles()?.filter { appDirFile ->
                        appDirFile.isFile && appDirFile.extension == "json"
                    }?.map { jsonFile ->
                        JsonUtil.deserializeApp(jsonFile).collect { app ->
                            if (!repository.doesExist(app.packageName))
                                repository.insert(app)
                        }
                    }
                }
                delay(1_500)
            }
        }
    }

    fun startRestoreWorker() {
        viewModelScope.launch(Dispatchers.Default) {
            val workerHelper = WorkerHelper(selectionList.toIntArray(), workManager)
            workerHelper.beginUniqueWork<MainWorker>(shouldBackup = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "RestoreViewModel cleared")
    }

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
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