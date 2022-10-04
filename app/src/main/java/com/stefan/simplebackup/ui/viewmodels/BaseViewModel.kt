package com.stefan.simplebackup.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SELECTION_EXTRA = "SELECTION_LIST"

sealed class BaseViewModel : ViewModel(),
    RecyclerViewStateSaver by RecyclerViewStateSaverImpl() {

    // Backup files observer
    protected val backupFilesObserver by lazy {
        BackupFilesObserver(
            rootDirPath = MainApplication.mainBackupDirPath,
            scope = viewModelScope,
            observableList = _observableList
        )
    }

    // Observable spinner properties used for progressbar observing
    protected var _spinner = MutableStateFlow(true)
    val spinner get() = _spinner.asStateFlow()

    // Observable application properties used for list loading
    protected var _observableList = MutableStateFlow(mutableListOf<AppData>())
    val observableList get() = _observableList.asStateFlow()

    fun setSpinning(shouldSpin: Boolean) {
        _spinner.value = shouldSpin
    }

    protected suspend fun loadList(
        shouldControlSpinner: Boolean = true,
        repositoryList: () -> Flow<MutableList<AppData>>
    ) {
        repositoryList().collect { list ->
            _observableList.value = list
            delay(400)
            if (shouldControlSpinner) _spinner.value = false
        }
    }
}