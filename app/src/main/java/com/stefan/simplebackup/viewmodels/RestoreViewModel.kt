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
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreViewModel(application: MainApplication) : AndroidViewModel(application) {

    lateinit var localApps: StateFlow<MutableList<AppData>>

    private val workManager = WorkManager.getInstance(application)
    private val repository = application.getRepository

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

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
            localApps = repository.localApps.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(2_000L),
                mutableListOf()
            )
            delay(400)
            _spinner.emit(false)
        }
    }

    suspend fun startPackagePolling() {
        withContext(Dispatchers.Default) {
            while (true) {
                findJsonFiles(mainBackupDirPath).collect { jsonFile ->
                    val app = deserializeApp(jsonFile)
                    app?.let {
                        repository.insert(app)
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