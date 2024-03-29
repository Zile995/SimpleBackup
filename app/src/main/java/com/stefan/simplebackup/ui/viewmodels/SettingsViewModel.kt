package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.manager.AppStorageManager
import com.stefan.simplebackup.utils.PreferenceHelper
import kotlinx.coroutines.launch

class SettingsViewModel(application: MainApplication) : ViewModel() {

    private val appStorageManager = AppStorageManager(application.applicationContext)

    init {
        Log.d("ViewModel", "SettingsViewModel created")
    }

    fun getUsedStorage() = appStorageManager.getUsedStorage()
    fun getTotalStorage() = appStorageManager.getTotalStorageSize()

    fun saveZipCompressionLevel(value: Float) {
        viewModelScope.launch {
            PreferenceHelper.saveZipCompressionLevel(value)
        }
    }

    fun setExcludeAppsCache(shouldExclude: Boolean) {
        viewModelScope.launch {
            PreferenceHelper.setExcludeAppsCache(shouldExclude)
        }
    }

    fun setDoublePressBackToExit(shouldDoublePress: Boolean) {
        viewModelScope.launch {
            PreferenceHelper.setDoublePressToExit(shouldDoublePress)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "SettingsViewModel cleared")
    }
}

class SettingsViewModelFactory(
    private val application: MainApplication,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unable to construct SettingsViewModel")
    }
}
