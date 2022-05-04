package com.stefan.simplebackup.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.repository.AppRepository
import com.stefan.simplebackup.utils.backup.WorkerHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val selectedApps: Array<String>?,
    application: MainApplication
) : AndroidViewModel(application) {

    private val repository: AppRepository = application.getRepository
    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(ioDispatcher) {
            startBackupWorker()
        }
    }

    private fun startBackupWorker() {
        selectedApps?.let {
            val workerHelper = WorkerHelper(selectedApps, workManager)
            workerHelper.startWorker(true)
        }
    }

    suspend fun getCurrentApp(packageName: String) =
        repository.getAppByPackageName(packageName)

    override fun onCleared() {
        super.onCleared()
        Log.d("ProgressViewModel", "ProgressViewModel cleared")
    }
}

@Suppress("UNCHECKED_CAST")
class ProgressViewModelFactory(
    private val selectedApps: Array<String>?,
    private val application: MainApplication
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(selectedApps, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}