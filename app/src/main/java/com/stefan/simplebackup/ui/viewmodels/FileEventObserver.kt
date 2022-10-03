package com.stefan.simplebackup.ui.viewmodels

import com.stefan.simplebackup.utils.file.RecursiveFileWatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface FileEventObserver<T> {

    val fileEventObserver: Flow<RecursiveFileWatcher.FileEvent>

    suspend fun observeFileEvents(observableList: MutableStateFlow<MutableList<T>>)

    suspend fun refreshFileList(
        observableList: MutableStateFlow<MutableList<T>>,
        filter: (T) -> Boolean
    )
}