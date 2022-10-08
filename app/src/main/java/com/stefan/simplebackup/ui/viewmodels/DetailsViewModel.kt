package com.stefan.simplebackup.ui.viewmodels

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.EventKind
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.asRecursiveFileWatcher
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class DetailsViewModel(
    val app: AppData?, application: MainApplication
) : ViewModel() {

    val appDatabase = AppDatabase.getInstance(
        application, application.applicationScope
    )

    private var _archNames = MutableStateFlow<List<String>?>(null)
    val archNames get() = _archNames.asStateFlow()

    init {
        Log.d("ViewModel", "DetailsViewModel created")
        getApkArchitectures()
    }

    inline fun observeLocalBackup(crossinline onBackupFileChanged: () -> Unit) {
        app?.apply {
            if (isLocal) {
                viewModelScope.launch {
                    val backupDir = File(FileUtil.localDirPath)
                    val fileEventObserver = backupDir.asRecursiveFileWatcher().processFileEvents()
                    fileEventObserver.collect { fileEvent ->
                        Log.d("ViewModel", "DetailsViewModel fileEvent = $fileEvent")
                        if (fileEvent.kind != EventKind.OVERFLOW)
                            onBackupFileChanged()
                    }
                }
            }
        }
    }

    private fun getApkArchitectures() {
        viewModelScope.launch(ioDispatcher) {
            app?.apply {
                _archNames.value = ZipUtil.getAppAbiList(this)
            }
        }
    }

    inline fun deleteLocalBackup(
        crossinline onSuccess: () -> Unit, crossinline onFailure: (message: String) -> Unit
    ) = viewModelScope.launch {
        app?.apply {
            try {
                FileUtil.deleteLocalBackup(packageName)
                onSuccess()
            } catch (e: IOException) {
                val message = "$e: ${e.message}"
                onFailure(message)
                Log.w("ViewModel", "Error occurred while deleting backup $message")
            }
        }
    }

    inline fun changeFavorites(
        crossinline onSuccess: (Boolean) -> Unit, crossinline onFailure: (message: String) -> Unit
    ) = viewModelScope.launch {
        app?.apply {
            try {
                val appRepository = AppRepository(appDatabase.appDao())
                appRepository.startRepositoryJob {
                    if (favorite)
                        removeFromFavorites(packageName)
                    else
                        addToFavorites(packageName)
                }.invokeOnCompletion {
                    favorite = !favorite
                    onSuccess(favorite)
                }
            } catch (e: SQLiteException) {
                onFailure("$e ${e.message}")
                Log.e("ViewModel", "Database exception: $e ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "DetailsViewModel cleared")
    }
}