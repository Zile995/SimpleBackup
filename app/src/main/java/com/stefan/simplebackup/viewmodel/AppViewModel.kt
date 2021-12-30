package com.stefan.simplebackup.viewmodel

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import com.stefan.simplebackup.data.AppBuilder
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.AppRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppRepository, private val appBuilder: AppBuilder) :
    ViewModel() {

    private lateinit var state: Parcelable

    private var _spinner = MutableLiveData(true)
    val spinner: LiveData<Boolean>
        get() = _spinner

    private lateinit var _allApps: LiveData<MutableList<Application>>
    val getAllApps: LiveData<MutableList<Application>>
        get() = _allApps

    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = ::state.isInitialized

    init {
        launchListLoading { getAllApps() }
        Log.d("viewmodel", "AppViewModel created")
    }

    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    fun insertApp(app: Application) = viewModelScope.launch {
        repository.insert(app)
    }

    fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
    }

    private fun getAllApps(): LiveData<MutableList<Application>> {
        return repository.getAllApps
    }

    suspend fun refreshPackageList() {
        viewModelScope.launch {
            appBuilder.getChangedPackageNames().collect { hashMap ->
                hashMap.forEach { entry ->
                    if (entry.value) {
                        with(appBuilder) {
                            getApp(getPackageApplicationInfo(entry.key)).collect {
                                insertApp(it)
                            }
                        }
                    } else {
                        deleteApp(entry.key)
                    }
                }
            }
        }
    }

    private fun launchListLoading(block: () -> LiveData<MutableList<Application>>) {
        viewModelScope.launch {
            try {
                _allApps = block()
            } catch (error: Throwable) {
                println(error.message)
            } finally {
                _spinner.postValue(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("viewmodel", "AppViewModel cleared")
    }
}

class AppViewModelFactory(private val repository: AppRepository, private val appBuilder: AppBuilder) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository, appBuilder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}