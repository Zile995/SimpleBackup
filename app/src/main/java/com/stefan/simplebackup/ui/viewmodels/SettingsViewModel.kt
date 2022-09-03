package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.utils.PreferenceHelper
import kotlinx.coroutines.launch

class SettingsViewModel(application: MainApplication) : ViewModel() {

    //private val appStorageManager = AppStorageManager(application.applicationContext)

    init {
        Log.d("ViewModel", "SettingsViewModel created")
    }

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

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "SettingsViewModel cleared")
    }
}