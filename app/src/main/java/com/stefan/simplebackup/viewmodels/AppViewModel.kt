package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.broadcasts.PackageListener
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.repository.AppRepository
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(application: MainApplication) :
    AndroidViewModel(application), PackageListener {

    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager

    // Observable application properties used for list loading
    private var _allApps = MutableStateFlow(mutableListOf<AppData>())
    val getAllApps: StateFlow<MutableList<AppData>>
        get() = _allApps

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
        appManager.printSequence()
        viewModelScope.launch {
            launchDataLoading {
                getAllAppsFromDatabase()
            }
            refreshPackageList()
        }
        Log.d("ViewModel", "AppViewModel created")
    }

    private fun getAllAppsFromDatabase() = repository.getAllApps

    // Loading methods
    private suspend inline fun launchDataLoading(
        crossinline allAppsFromDatabase: () -> Flow<MutableList<AppData>>,
    ) {
        runCatching {
            allAppsFromDatabase().collectLatest { apps ->
                _allApps.value = apps
                delay(150)
                _spinner.value = false
            }
        }.onFailure { throwable ->
            throwable.message?.let { message -> Log.e("ViewModel", message) }
        }
    }

    // Repository methods
    private fun insertApp(app: AppData) = viewModelScope.launch {
        repository.insert(app)
        appManager.updateSequenceNumber()
    }

    private fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
        appManager.updateSequenceNumber()
    }

    // Used to check for changed packages on init
    fun refreshPackageList() {
        Log.d("ViewModel", "Refreshing the package list")
        viewModelScope.launch(ioDispatcher) {
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
            build(packageName).collect { app ->
                insertApp(app)
            }
        }
    }

    override suspend fun deletePackage(packageName: String) {
        deleteApp(packageName)
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