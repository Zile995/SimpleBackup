package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import com.stefan.simplebackup.data.repository.AppRepository
import com.stefan.simplebackup.utils.main.launchWithLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppViewModel(application: MainApplication) :
    BaseAndroidViewModel(application), PackageListener by PackageListenerImpl(application) {

    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    // Observable application properties used for list loading
    val installedApps by lazy {
        repository.installedApps.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(4_000L),
            mutableListOf()
        )
    }

    init {
        viewModelScope.launchWithLogging {
            appManager.printSequence()
            installedApps
            delay(500)
            _spinner.emit(false)
            refreshPackageList()
        }
    }

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
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