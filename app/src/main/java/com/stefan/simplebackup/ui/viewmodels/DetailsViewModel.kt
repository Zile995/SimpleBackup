package com.stefan.simplebackup.ui.viewmodels

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.asRecursiveFileWatcher
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class DetailsViewModel(
    val app: AppData?,
    val application: MainApplication,
) : ViewModel() {

    val backupFileEvents by lazy {
        val backupDir = File(FileUtil.localDirPath)
        backupDir.asRecursiveFileWatcher(viewModelScope).fileEvent
    }

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _archNames = MutableStateFlow<List<String>?>(null)
    val archNames get() = _archNames.asStateFlow()

    init {
        Log.d("ViewModel", "DetailsViewModel created")
        getApkArchitectures()
    }

    private fun getApkArchitectures() {
        viewModelScope.launch(ioDispatcher) {
            app?.apply {
                _archNames.value = ZipUtil.getAppAbiList(this)
            }
        }
    }

    fun getApkSizes() {
        app?.apply {
            // TODO: Get stored or calculated sizes
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
                onFailure(e.toString())
                Log.w("ViewModel", "Error occurred while deleting backup $e")
            }
        }
    }

    inline fun changeFavorites(
        crossinline onSuccess: (Boolean) -> Unit, crossinline onFailure: (message: String) -> Unit
    ) = viewModelScope.launch {
        app?.apply {
            try {
                val appDatabase = AppDatabase.getInstance(
                    application, application.applicationScope
                )
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
                onFailure(e.toString())
                Log.e("ViewModel", "Database exception: $e")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "DetailsViewModel cleared")
    }
}

class DetailsViewModelFactory(
    private val app: AppData?,
    private val application: MainApplication,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailsViewModel(app, application) as T
        }
        throw IllegalArgumentException("Unable to construct DetailsViewModel")
    }
}