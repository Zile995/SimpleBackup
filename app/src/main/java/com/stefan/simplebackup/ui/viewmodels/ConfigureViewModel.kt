package com.stefan.simplebackup.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.utils.extensions.observeNetworkConnection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ConfigureViewModel(application: Application) : AndroidViewModel(application) {

    val hasInternetConnection by lazy {
        application.observeNetworkConnection(delay = 500L)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(1_000L),
                initialValue = false
            )
    }

    init {
        Log.d("ViewModel", "ConfigureViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "ConfigureViewModel cleared")
    }
}

class ConfigureViewModelFactory : ViewModelProvider.AndroidViewModelFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ConfigureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfigureViewModel(extras[APPLICATION_KEY] as MainApplication) as T
        }
        throw IllegalArgumentException("Unable to construct ConfigureViewModel")
    }
}