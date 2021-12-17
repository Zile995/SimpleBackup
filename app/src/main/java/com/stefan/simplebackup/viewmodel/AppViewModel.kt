package com.stefan.simplebackup.viewmodel

import androidx.lifecycle.*
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.AppRepository
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    private var _allApps: LiveData<MutableList<Application>> = repository.getAllApps
    val getAllApps: LiveData<MutableList<Application>>
        get() = _allApps

    fun insertApp(app: Application) = viewModelScope.launch {
        repository.insert(app)
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