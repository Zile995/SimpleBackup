package com.stefan.simplebackup.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.AppRepository
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    private var _spinner = MutableLiveData(false)
    val spinner: LiveData<Boolean>
        get() = _spinner

    private var _allApps: LiveData<MutableList<Application>> = getAllApps()
    val getAllApps: LiveData<MutableList<Application>>
        get() = _allApps


    fun insertApp(app: Application) = viewModelScope.launch {
        repository.insert(app)
    }

    private fun getAllApps(): LiveData<MutableList<Application>> {
        val allApps = repository.getAllApps
        _spinner.postValue(true)
        return allApps
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