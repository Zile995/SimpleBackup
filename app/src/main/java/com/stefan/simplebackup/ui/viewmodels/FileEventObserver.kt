package com.stefan.simplebackup.ui.viewmodels

import com.stefan.simplebackup.utils.file.RecursiveFileWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface FileEventObserver<T> {

    val fileEventObserver: Flow<RecursiveFileWatcher.FileEvent>

    fun observeFilesEvents(
        scope: CoroutineScope,
        observable: MutableStateFlow<MutableList<T>>
    )
}