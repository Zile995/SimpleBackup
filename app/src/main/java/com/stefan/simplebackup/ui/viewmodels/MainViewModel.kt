package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.selectionFinished
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MainViewModel(application: MainApplication) : ViewModel(),
    PackageListener by PackageListenerImpl(application) {

    var isAppBarExpanded = true

    private var _isSearching = MutableStateFlow(false)
    val isSearching get() = _isSearching.asStateFlow()

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val selectionList = mutableListOf<Int>()
    val isSelected = _isSelected.asStateFlow()
    val setSelectionMode: SelectionModeCallBack = { isSelected: Boolean ->
        _isSelected.value = isSelected
        if (!isSelected) selectionFinished = true
        println("selectionList = $selectionList")
    }

    fun setSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
    }

    fun changeFavorites() {
        Log.d("ViewModel", "Calling changeFavorites")
        viewModelScope.launch(ioDispatcher) {
            val semaphore = Semaphore(5)
            selectionList.forEach { uid ->
                semaphore.withPermit {
                    launch {
                        repository.changeFavorites(uid)
                    }
                }
            }
        }
    }

    init {
        Log.d("ViewModel", "MainViewModel created")
        viewModelScope.launch(ioDispatcher) {
            refreshPackageList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "MainViewModel cleared")
    }
}