package com.stefan.simplebackup.viewmodel

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import com.stefan.simplebackup.adapter.SelectionListener
import com.stefan.simplebackup.broadcasts.PackageListener
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.database.AppRepository
import com.stefan.simplebackup.database.DatabaseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: DatabaseApplication) :
    ViewModel(), PackageListener, SelectionListener {
    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager

    private var _isSelected = MutableLiveData(false)
    val isSelected: LiveData<Boolean> get() = _isSelected
    private val selectionList = mutableListOf<AppData>()

    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = ::state.isInitialized

    var _spinner = MutableLiveData(true)
    val spinner: LiveData<Boolean>
        get() = _spinner

    private lateinit var allApps: LiveData<MutableList<AppData>>
    val getAllApps: LiveData<MutableList<AppData>>
        get() = allApps

    init {
        appManager.printSequence()
        launchListLoading { getAllAppsFromRepository() }
        Log.d("ViewModel", "AppViewModel created")
    }

    private fun launchListLoading(block: () -> LiveData<MutableList<AppData>>) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                allApps = block()
            }.onSuccess {
                checkIfLoaded()
                _spinner.postValue(false)
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("ViewModel", message) }
            }
        }
    }

    private suspend fun checkIfLoaded() {
        withContext(Dispatchers.Default) {
            val numberOfInstalled = appManager.getNumberOfInstalled()
            while (true) {
                val numberOfStored = getAllApps.value?.size ?: 0
                if (numberOfStored == numberOfInstalled)
                    break
            }
        }
    }

    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    override fun setSelectionMode(selection: Boolean) {
        _isSelected.postValue(selection)
    }

    override fun hasSelectedItems(): Boolean = _isSelected.value ?: false

    override fun setSelectedItems(selectedList: MutableList<AppData>) {
        selectionList.clear()
        selectionList.addAll(selectedList)
    }

    override fun getSelectedItems(): MutableList<AppData> {
        return selectionList
    }

    override fun addSelectedItem(app: AppData) {
        selectionList.add(app)
    }

    override fun removeSelectedItem(app: AppData) {
        selectionList.remove(app)
    }

    private fun insertApp(app: AppData) = viewModelScope.launch {
        repository.insert(app)
        appManager.updateSequenceNumber()
    }

    private fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
        appManager.updateSequenceNumber()
    }

    private fun getAllAppsFromRepository(): LiveData<MutableList<AppData>> {
        return repository.getAllApps
    }

    suspend fun refreshPackageList() {
        viewModelScope.launch(Dispatchers.IO) {
            appManager.apply {
                getChangedPackageNames().collect { packageName ->
                    if (doesPackageExists(packageName)) {
                        build(packageName).collect { app ->
                            insertApp(app)
                        }
                    } else {
                        deleteApp(packageName)
                    }
                }
            }
        }
    }

    override suspend fun addOrUpdatePackage(packageName: String) {
        viewModelScope.launch {
            appManager.apply {
                build(packageName).collect { app ->
                    insertApp(app)
                }
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