package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.utils.extensions.filterBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class FavoritesViewModel(
    repository: AppRepository, appDataType: AppDataType?
) : BaseViewModel() {

    init {
        viewModelScope.launch {
            appDataType?.let {
                when (appDataType) {
                    AppDataType.USER -> {
                        loadList {
                            repository.installedApps.filterBy {
                                it.favorite
                            }
                        }
                    }
                    AppDataType.LOCAL -> {
                        backupFilesObserver.apply {
                            refreshBackupFileList { it.favorite }
                            observeBackupFiles()
                        }
                    }
                    AppDataType.CLOUD -> {
                        loadList {
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