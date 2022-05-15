package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.repository.AppRepository
import com.stefan.simplebackup.utils.main.ioDispatcher
import com.stefan.simplebackup.utils.main.launchWithLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(application: MainApplication) :
    AndroidViewModel(application), PackageListener {

    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    //     Observable application properties used for list loading
    private var _allApps = MutableStateFlow(mutableListOf<AppData>())
    val getAllApps: StateFlow<MutableList<AppData>>
        get() = _allApps

    // Selection properties
    val selectionList = mutableListOf<Int>()
    val setSelectionMode: (Boolean) -> Unit = { isSelected -> _isSelected.value = isSelected }
    private var _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> get() = _isSelected

    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = this::state.isInitialized

    init {
        viewModelScope.launchDataLoading {
            repository.installedApps
        }
    }

    // Loading methods
    private inline fun CoroutineScope.launchDataLoading(
        crossinline databaseCallBack: () -> Flow<MutableList<AppData>>,
    ) {
        launch {
            appManager.printSequence()
            databaseCallBack().collect { apps ->
                _allApps.value = apps
                delay(200)
                _spinner.emit(false)
                refreshPackageList()
            }
        }
    }

    // Used to check for changed packages on init
    suspend fun refreshPackageList() {
        Log.d("ViewModel", "Refreshing the package list")
        withContext(ioDispatcher) {
            appManager.apply {
                getChangedPackageNames().collect { packageName ->
                    if (doesPackageExists(packageName)) {
                        Log.d("ViewModel", "Adding or updating the $packageName")
                        addOrUpdatePackage(packageName)
                    } else {
                        Log.d("ViewModel", "Deleting the $packageName")
                        deletePackage(packageName)
                    }
                }
            }
        }
    }

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    // PackageListener methods - Used for database package updates
    override suspend fun addOrUpdatePackage(packageName: String) {
        appManager.apply {
            repository.insert(build(packageName))
            updateSequenceNumber()
        }
    }

    override suspend fun deletePackage(packageName: String) {
        repository.delete(packageName)
        appManager.updateSequenceNumber()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "AppViewModel cleared")
    }
}

class AppViewModelFactory(
    private val application: MainApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}