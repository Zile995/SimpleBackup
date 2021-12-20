package com.stefan.simplebackup.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.AppRepository
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    private var _spinner = MutableLiveData(true)
    val spinner: LiveData<Boolean>
        get() = _spinner

    private lateinit var _allApps: LiveData<MutableList<Application>>
    val getAllApps: LiveData<MutableList<Application>>
        get() = _allApps

    init {
        launchListLoading { getAllApps() }
    }

    fun insertApp(app: Application) = viewModelScope.launch {
        repository.insert(app)
    }

    private fun getAllApps(): LiveData<MutableList<Application>> {
        return repository.getAllApps
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

    init {
        Log.d("viewmodel", "AppViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("viewmodel", "AppViewModel cleared")
    }
}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}