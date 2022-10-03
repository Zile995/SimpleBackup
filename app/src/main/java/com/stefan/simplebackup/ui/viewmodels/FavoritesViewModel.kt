package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.AppDataType
import kotlinx.coroutines.launch

class FavoritesViewModel(
    repository: AppRepository,
    appDataType: AppDataType?
) : BaseViewModel(),
    FileEventObserver<AppData> by BackupFilesObserver(rootDirPath = mainBackupDirPath) {

    init {
        viewModelScope.launch {
            appDataType?.let {
                when (appDataType) {
                    AppDataType.USER -> {
                        loadList(false) { repository.installedApps }
                    }
                    AppDataType.LOCAL -> {
                        refreshFileList(_observableList) { it.favorite }
                        observeFileEvents(_observableList)
                    }
                    AppDataType.CLOUD -> {
                        loadList(false) {
                            repository.installedApps
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