package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.utils.extensions.filterBy
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.launch

class FavoritesViewModel(
    repository: AppRepository,
    appDataType: AppDataType?
) : BaseViewModel() {

    init {
        viewModelScope.launch(ioDispatcher) {
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
                        loadList(false) {
                            repository.localApps.filterBy {
                                it.favorite
                            }
                        }
                    }
                    AppDataType.CLOUD -> {
                        loadList(false) {
                            repository.cloudApps.filterBy {
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