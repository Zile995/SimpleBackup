package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.ui.fragments.viewpager.FavoriteType
import com.stefan.simplebackup.utils.extensions.filterBy
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.launch

class FavoritesViewModel(
    repository: AppRepository,
    favoriteType: FavoriteType?,
    shouldControlSpinner: Boolean = false
) : BaseViewModel(shouldControlSpinner) {

    init {
        viewModelScope.launch(ioDispatcher) {
            favoriteType?.let {
                when (favoriteType) {
                    FavoriteType.USER -> {
                        loadList {
                            repository.installedApps.filterBy {
                                it.favorite
                            }
                        }
                    }
                    FavoriteType.LOCAL -> {
                        loadList {
                            repository.localApps.filterBy {
                                it.favorite
                            }
                        }
                    }
                    FavoriteType.CLOUD -> {
                        loadList {
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