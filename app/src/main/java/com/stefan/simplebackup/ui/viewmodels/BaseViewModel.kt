package com.stefan.simplebackup.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.BackupFilesObserver
import com.stefan.simplebackup.utils.file.FileUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SELECTION_EXTRA = "SELECTION_LIST"

abstract class BaseViewModel : ViewModel(), RecyclerViewStateSaver by RecyclerViewStateSaverImpl() {
    // Backup files observer
    protected val backupFilesObserver by lazy {
        BackupFilesObserver(
            rootDirPath = FileUtil.localDirPath,
            scope = viewModelScope,
            observableList = _observableList
        )
    }

    // Observable spinner properties used for progressbar observing
    protected val _spinner = MutableStateFlow(true)
    val spinner get() = _spinner.asStateFlow()

    // Observable application properties used for list loading
    private var _observableList = MutableStateFlow(mutableListOf<AppData>())
    val observableList get() = _observableList.asStateFlow()

    fun setSpinning(shouldSpin: Boolean) {
        _spinner.value = shouldSpin
    }

    protected suspend fun loadList(
        shouldControlSpinner: Boolean = true, repositoryList: () -> Flow<MutableList<AppData>>
    ) {
        repositoryList().collect { list ->
            _observableList.value = list
            delay(400)
            if (shouldControlSpinner) _spinner.value = false
        }
    }
}