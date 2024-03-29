package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.utils.extensions.filterBy
import kotlinx.coroutines.launch

class FavoritesViewModel(repository: AppRepository) : BaseViewModel() {

    init {
        viewModelScope.launch {
            loadList(false) {
                repository.installedApps.filterBy { it.isFavorite }
            }
        }
        Log.d("ViewModel", "FavoritesViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "FavoritesViewModel cleared")
    }
}

class FavoritesViewModelFactory {
    val factory: (repository: AppRepository) -> ViewModelProvider.Factory = { repository ->
        viewModelFactory {
            initializer {
                FavoritesViewModel(repository)
            }
        }
    }
}