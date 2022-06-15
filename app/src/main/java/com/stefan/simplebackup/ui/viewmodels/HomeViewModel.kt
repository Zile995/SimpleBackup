package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import com.stefan.simplebackup.utils.extensions.launchWithLogging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: MainApplication) :
    BaseViewModel(application),
    PackageListener by PackageListenerImpl(application) {
    private val repository = getRepository()

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    // Observable application properties used for list loading
    val installedApps = repository.installedApps.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(4_000L),
        mutableListOf()
    )

    init {
        Log.d("ViewModel", "HomeViewModel created")
        viewModelScope.launchWithLogging(CoroutineName("LoadHomeList")) {
            delay(400)
            _spinner.emit(false)
            refreshPackageList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "HomeViewModel cleared")
    }
}

class HomeViewModelFactory(
    private val application: MainApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}