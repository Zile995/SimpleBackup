package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.selectionFinished
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.root.RootChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.system.measureTimeMillis

class MainViewModel(application: MainApplication) : ViewModel(),
    PackageListener by PackageListenerImpl(application) {
    var isAppBarExpanded = true

    private var rootChecked = PreferenceHelper.isRootChecked
    private val rootChecker = RootChecker(application.applicationContext)

    private var _isSearching = MutableStateFlow(false)
    val isSearching get() = _isSearching.asStateFlow()

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val selectionList = mutableListOf<String>()
    val isSelected = _isSelected.asStateFlow()
    val setSelectionMode: SelectionModeCallBack = { isSelected: Boolean ->
        _isSelected.value = isSelected
        if (!isSelected) selectionFinished = true
    }

    init {
        Log.d("ViewModel", "MainViewModel created")
        viewModelScope.launch(ioDispatcher) {
            refreshPackageList()
        }
    }

    suspend fun onRootCheck(
        onRootNotGranted: () -> Unit,
        onDeviceNotRooted: () -> Unit
    ) {
        if (rootChecked) return
        if (rootChecker.hasRootAccess() == false && rootChecker.isDeviceRooted())
            onRootNotGranted()
        if (!rootChecker.isDeviceRooted())
            onDeviceNotRooted()
        rootChecked = true
        PreferenceHelper.setRootChecked(true)
    }

    fun setSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
    }

    fun changeFavorites() {
        Log.d("ViewModel", "Calling changeFavorites")
        viewModelScope.launch(ioDispatcher) {
            val time = measureTimeMillis {
                val semaphore = Semaphore(5)
                selectionList.forEach { packageName ->
                    semaphore.withPermit {
                        launch {
                            repository.changeFavorites(packageName)
                        }
                    }
                }
            }
            Log.d("ViewModel", "Finished changing favorites in $time ms")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "MainViewModel cleared")
    }
}