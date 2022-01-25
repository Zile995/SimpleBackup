package com.stefan.simplebackup.viewmodel

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import com.stefan.simplebackup.adapter.SelectionListener
import com.stefan.simplebackup.broadcasts.BroadcastListener
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.AppRepository
import com.stefan.simplebackup.database.DatabaseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppViewModel(application: DatabaseApplication) :
    ViewModel(), BroadcastListener, SelectionListener {
    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager

    private var _isSelected = MutableLiveData(false)
    val isSelected: LiveData<Boolean> get() = _isSelected
    private val selectionList = mutableListOf<Application>()

    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = ::state.isInitialized

    private var _spinner = MutableLiveData(true)
    val spinner: LiveData<Boolean>
        get() = _spinner

    private lateinit var _allApps: LiveData<MutableList<Application>>
    val getAllApps: LiveData<MutableList<Application>>
        get() = _allApps

    init {
        launchListLoading { getAllAppsFromRepository() }
        Log.d("ViewModel", "AppViewModel created")
    }

    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    override fun setSelection(selection: Boolean) {
        _isSelected.postValue(selection)
    }

    override fun isSelected(): Boolean = _isSelected.value ?: false

    override fun getSelected(): MutableList<Application> {
        return selectionList
    }

    override fun addSelection(app: Application) {
        selectionList.add(app)
    }

    override fun removeSelection(app: Application) {
        selectionList.remove(app)
    }

    private fun insertApp(app: Application) = viewModelScope.launch {
        repository.insert(app)
    }

    private fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
    }

    private fun getAllAppsFromRepository(): LiveData<MutableList<Application>> {
        return repository.getAllApps
    }

    suspend fun refreshPackageList() {
        viewModelScope.launch(Dispatchers.IO) {
            appManager.getChangedPackageNames().collect { packageName ->
                if (appManager.doesPackageExists(packageName)) {
                    appManager.build(packageName).collect { app ->
                        insertApp(app)
                    }
                } else {
                    deleteApp(packageName)
                }
            }
        }
    }

    override suspend fun addOrUpdatePackage(packageName: String) {
        with(appManager) {
            build(packageName).collect { app ->
                insertApp(app)
            }
        }
    }

    override suspend fun deletePackage(packageName: String) {
        deleteApp(packageName)
    }

    private fun launchListLoading(block: () -> LiveData<MutableList<Application>>) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                _allApps = block()
            }.onSuccess {
                _spinner.postValue(false)
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("ViewModel", message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "AppViewModel cleared")
    }
}

class AppViewModelFactory(
    private val application: DatabaseApplication
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}