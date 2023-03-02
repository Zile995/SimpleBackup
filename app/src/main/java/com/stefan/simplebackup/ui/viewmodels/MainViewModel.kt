package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.receivers.PackageListenerImpl
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.filterBy
import com.stefan.simplebackup.utils.root.RootChecker
import com.stefan.simplebackup.utils.work.FileUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

class MainViewModel(application: MainApplication) : AndroidViewModel(application),
    PackageListener by PackageListenerImpl(application) {

    var isAppBarExpanded: Boolean = true
        private set

    // Dispatchers
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // Root checking
    private val rootChecker = RootChecker(application)
    private val hasCheckedRootGranted get() = PreferenceHelper.hasCheckedRootGranted
    private val hasCheckedDeviceRooted get() = PreferenceHelper.hasCheckedDeviceRooted

    // Ui actions
    val action: (UiAction) -> Unit

    // Search input
    private var _searchInput = MutableLiveData<String?>()
    val searchInput: LiveData<String?> get() = _searchInput

    // Search
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val isSelected = _isSelected.asStateFlow()

    // Settings destination
    private val _isSettingsDestination = MutableStateFlow(false)
    val isSettingsDestination = _isSettingsDestination.asStateFlow()

    // Selection properties
    val selectionList = mutableListOf<String>()
    val setSelectionMode: SelectionModeCallBack = { isSelected: Boolean ->
        _isSelected.value = isSelected
    }


    init {
        action = { uiAction ->
            when (uiAction) {
                is UiAction.Search -> {
                    setSearchInput(uiAction.query)
                }
                is UiAction.ChangeAppBarState -> {
                    isAppBarExpanded = uiAction.isExpanded
                }
                is UiAction.ChangeSearchBarState -> {
                    _isSearching.value = uiAction.search
                    _isSettingsDestination.value = uiAction.settings
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

    suspend fun onRootCheck(onRootNotGranted: () -> Unit, onDeviceNotRooted: () -> Unit) {
        val hasRootAccess = rootChecker.hasRootAccess()
        if (hasRootAccess == true) return
        val isDeviceRooted = rootChecker.isDeviceRooted()
        if (hasRootAccess == false && isDeviceRooted && !hasCheckedRootGranted) {
            onRootNotGranted()
            PreferenceHelper.setCheckedRootGranted(true)
        }
        if (!isDeviceRooted && !hasCheckedDeviceRooted) {
            onDeviceNotRooted()
            PreferenceHelper.setCheckedDeviceRooted(true)
        }
    }

    fun setSearchInput(text: String?) {
        if (text.isNullOrBlank() || text.isEmpty() || text.contains("%") || text.contains("_")) {
            resetSearchInput()
            return
        }
        _searchInput.value = text
    }

    suspend fun findAppsByName(name: String, isLocal: Boolean) =
        withContext(viewModelScope.coroutineContext + ioDispatcher) {
            if (name.isEmpty()) flowOf(mutableListOf())
            else repository.findAppsByName(name, isLocal).filterBy { it.isUserApp }
        }

    fun resetSearchInput() {
        _searchInput.value = ""
    }

    inline fun addToFavorites(
        crossinline onSuccess: (number: Int) -> Unit,
        crossinline onFailure: (message: String) -> Unit
    ) = viewModelScope.launch {
        try {
            repository.startRepositoryJob { ioScope: CoroutineScope ->
                selectionList.forEach { packageName ->
                    ioScope.launch {
                        addToFavorites(packageName)
                    }
                }
            }.invokeOnCompletion {
                onSuccess(selectionList.size)
            }
        } catch (e: Exception) {
            onFailure(e.toString())
            Log.w("ViewModel", "Error occurred while removing favorites $e")
        }
    }

    inline fun removeFromFavorites(
        crossinline onSuccess: (number: Int) -> Unit,
        crossinline onFailure: (message: String) -> Unit
    ) = viewModelScope.launch {
        try {
            repository.startRepositoryJob { ioScope: CoroutineScope ->
                selectionList.forEach { packageName ->
                    ioScope.launch {
                        removeFromFavorites(packageName)
                    }
                }
            }.invokeOnCompletion {
                onSuccess(selectionList.size)
            }
        } catch (e: Exception) {
            onFailure(e.toString())
            Log.w("ViewModel", "Error occurred while removing favorites $e")
        } finally {
            delay(200)
            setSelectionMode(false)
        }
    }

    inline fun deleteSelectedBackups(
        crossinline onSuccess: () -> Unit, crossinline onFailure: (message: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Convert toSet, to avoid concurrent modifications.
                selectionList.toSet().forEach { packageName ->
                    FileUtil.deleteLocalBackup(packageName)
                }
                onSuccess()
            } catch (e: IOException) {
                onFailure(e.toString())
                Log.w("ViewModel", "Error occurred while deleting backups $e")
            } finally {
                delay(200)
                setSelectionMode(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "MainViewModel cleared")
    }
}

sealed class UiAction {
    data class Search(val query: String?) : UiAction()
    data class ChangeAppBarState(val isExpanded: Boolean) : UiAction()
    data class ChangeSearchBarState(
        val search: Boolean,
        val settings: Boolean
    ) : UiAction()
}

class MainViewModelFactory {
    val factory = viewModelFactory {
        initializer {
            val application = (this[APPLICATION_KEY]) as MainApplication
            MainViewModel(application)
        }
    }
}