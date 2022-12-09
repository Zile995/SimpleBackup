package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.launchWithLogging
import kotlinx.coroutines.CoroutineName

open class HomeViewModel(
    private val packageListener: PackageListener
) : BaseViewModel() {

    protected val repository = packageListener.repository
    suspend fun refreshPackages() = packageListener.refreshPackageList()

    init {
        Log.d("ViewModel", "HomeViewModel created")
        viewModelScope.launchWithLogging(CoroutineName("LoadHomeList")) {
            loadList {
                repository.installedApps
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "HomeViewModel cleared")
    }
}

class HomeViewModelFactory(
    private val packageListener: PackageListener,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(packageListener) as T
        }
        throw IllegalArgumentException("Unable to construct HomeViewModel")
    }
}