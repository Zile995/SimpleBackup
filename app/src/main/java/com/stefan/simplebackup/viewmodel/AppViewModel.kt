package com.stefan.simplebackup.viewmodel

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import com.stefan.simplebackup.broadcasts.BroadcastListener
import com.stefan.simplebackup.data.AppBuilder
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.AppRepository
import com.stefan.simplebackup.database.DatabaseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: DatabaseApplication) :
    ViewModel(), BroadcastListener {
    private val repository: AppRepository = application.getRepository
    private val appBuilder: AppBuilder = application.getAppBuilder

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
        launchListLoading { getAllApps() }
        Log.d("viewmodel", "AppViewModel created")
    }

    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    private fun insertApp(app: Application) = viewModelScope.launch {
        repository.insert(app)
    }

    private fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
    }

    private fun getAllApps(): LiveData<MutableList<Application>> {
        return repository.getAllApps
    }

    suspend fun refreshPackageList() {
        viewModelScope.launch(Dispatchers.IO) {
            appBuilder.getChangedPackageNames().collect { packageName ->
                if (appBuilder.doesPackageExists(packageName)) {
                    appBuilder.build(packageName).collect { app ->
                        insertApp(app)
                    }
                } else {
                    deleteApp(packageName)
                }
            }
        }
    }

    override suspend fun addOrUpdatePackage(packageName: String) {
        with(appBuilder) {
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
            try {
                _allApps = block()
            } catch (error: Throwable) {
                println(error.message)
            } finally {
                withContext(Dispatchers.Main) {
                    _spinner.postValue(false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("viewmodel", "AppViewModel cleared")
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