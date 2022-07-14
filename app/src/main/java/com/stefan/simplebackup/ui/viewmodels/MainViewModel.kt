package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: MainApplication) : ViewModel(),
    PackageListener by PackageListenerImpl(application) {

    var isAppBarExpanded = true

    private var _isSearching = MutableStateFlow(false)
    val isSearching get() = _isSearching.asStateFlow()

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val selectionList = mutableListOf<Int>()
    val isSelected: StateFlow<Boolean> get() = _isSelected
    val setSelectionMode: SelectionModeCallBack =
        { isSelected: Boolean -> _isSelected.value = isSelected }

    fun setSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
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