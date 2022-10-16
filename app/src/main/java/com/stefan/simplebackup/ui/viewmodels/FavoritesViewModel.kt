package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.utils.extensions.filterBy
import kotlinx.coroutines.launch

class FavoritesViewModel(
    repository: AppRepository, appDataType: AppDataType?
) : BaseViewModel() {

    init {
        viewModelScope.launch {
            appDataType?.let {
                when (appDataType) {
                    AppDataType.USER -> {
                        loadList(false) {
                            repository.installedApps.filterBy {
                                it.favorite
                            }
                        }
                    }
                    AppDataType.LOCAL -> {

                    }
                    AppDataType.CLOUD -> {
                        loadList(false) {
                            repository.installedApps.filterBy {
                                it.favorite
                            }
                        }
                    }
                }
            }
        }
        Log.d("ViewModel", "FavoritesViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "FavoritesViewModel cleared")
    }
}

class FavoritesViewModelFactory(
    private val repository: AppRepository,
    private val appDataType: AppDataType?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoritesViewModel(repository, appDataType) as T
        }
        throw IllegalArgumentException("Unable to construct FavoritesViewModel")
    }
}