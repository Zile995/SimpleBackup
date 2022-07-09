package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: MainApplication) : ViewModel(),
    PackageListener by PackageListenerImpl(application) {

    private var _isSearching = MutableStateFlow(false)
    val isSearching get() = _isSearching.asStateFlow()

    private var _shouldDisableTab = MutableStateFlow(false)
    val shouldDisableTab get() = _shouldDisableTab.asStateFlow()

    fun setSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
    }

    fun changeTab(shouldEnable: Boolean) {
        _shouldDisableTab.value = shouldEnable
    }

    init {
        viewModelScope.launch {
            refreshPackageList()
        }
        Log.d("ViewModel", "MainViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "MainViewModel cleared")
    }
}