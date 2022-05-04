package com.stefan.simplebackup.viewmodels

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.utils.backup.WorkerHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

    // Selection properties
    val selectionList = mutableListOf<String>()
    val setSelectionMode: (Boolean) -> Unit = { isSelected -> _isSelected.value = isSelected }
    private var _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> get() = _isSelected

    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = this::state.isInitialized

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    init {
        Log.d("ViewModel", "RestoreViewModel created")
    }

    fun startRestoreWorker() {
        viewModelScope.launch(ioDispatcher) {
            val workerHelper = WorkerHelper(selectionList.toTypedArray(), workManager)
            workerHelper.startWorker(shouldBackup = false)
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

class RestoreViewModelFactory(private val application: Application) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(RestoreViewModel::class.java)) {
            return RestoreViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}