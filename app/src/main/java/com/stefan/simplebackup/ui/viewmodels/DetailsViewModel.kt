package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class DetailsViewModel(
    val app: AppData?, application: MainApplication
) : ViewModel() {

    private val appRepository by lazy {
        AppRepository(
            AppDatabase.getInstance(
                application.applicationContext, application.applicationScope
            ).appDao()
        )
    }

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

    inline fun deleteLocalBackup(
        crossinline onSuccess: () -> Unit, crossinline onFailure: (message: String) -> Unit
    ) = viewModelScope.launch {
        app?.apply {
            try {
                FileUtil.deleteLocalBackup(packageName)
                onSuccess()
            } catch (e: IOException) {
                onFailure("$e ${e.message}")
                Log.w("ViewModel", "Error occurred while deleting backup $e: ${e.message}")
            }
        }
    }

    fun changeFavorites() = viewModelScope.launch {
        try {
            app?.apply {
                _favoriteChanged.value = null
                appRepository.startRepositoryJob {
                    if (favorite) removeFromFavorites(packageName)
                    else addToFavorites(packageName)
                }.invokeOnCompletion {
                    favorite = !favorite
                    _favoriteChanged.value = true
                }
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Database exception: $e")
            _favoriteChanged.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "DetailsViewModel cleared")
    }
}