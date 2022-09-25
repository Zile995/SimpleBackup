package com.stefan.simplebackup.ui.viewmodels

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
    val app: AppData?,
    application: MainApplication
) : ViewModel() {

    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val appRepository by lazy {
        AppRepository(AppDatabase.getInstance(application).appDao())
    }
    private val workManager by lazy { WorkManager.getInstance(application) }

    private var _archNames = MutableStateFlow<List<String>?>(null)
    val archNames get() = _archNames.asStateFlow()

    private var _favoriteChanged = MutableStateFlow<Boolean?>(null)
    val favoriteChanged get() = _favoriteChanged.asStateFlow()

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

    fun changeFavorites() =
        viewModelScope.launch {
            try {
                app?.apply {
                    _favoriteChanged.value = null
                    appRepository.startRepositoryJob {
                        if (favorite)
                            removeFromFavorites(packageName)
                        else
                            addToFavorites(packageName)
                    }.invokeOnCompletion {
                        favorite = !favorite
                        _favoriteChanged.value = true
                    }
                }
            } catch (e: SQLiteException) {
                Log.e("ViewModel", "Database exception: $e")
                _favoriteChanged.value = false
            }
        }

    fun createLocalBackup() {
        viewModelScope.launch(defaultDispatcher) {
            app?.apply {
                val workerHelper = WorkerHelper(packageName, workManager)
                workerHelper.beginUniqueWork<MainWorker>()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "DetailsViewModel cleared")
    }
}