package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.utils.file.BackupFilesObserver
import com.stefan.simplebackup.utils.work.FileUtil

class SearchViewModel(appRepository: AppRepository) : ViewModel(),
    RecyclerViewStateSaver by RecyclerViewStateSaverImpl() {

    var checkedChipPosition: Int = 0
        private set

    private val backupFilesObserver = BackupFilesObserver(
        rootDirPath = FileUtil.localDirPath,
        scope = viewModelScope,
        appRepository = appRepository
    )

    init {
        Log.d("ViewModel", "SearchViewModel created")
        backupFilesObserver.refreshBackupList()
        backupFilesObserver.observeBackupFiles()
    }

    fun saveCheckedChipPosition(position: Int) {
        checkedChipPosition = position
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "SearchViewModel cleared")
    }
}

class SearchViewModelFactory(private val appRepository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(appRepository) as T
        }
        throw IllegalArgumentException("Unable to construct SearchViewModel")
    }
}