package com.stefan.simplebackup.viewmodels

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.domain.repository.AppRepository
import com.stefan.simplebackup.utils.backup.BackupWorkerHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val selectedApps: Array<String>?,
    application: MainApplication
) : AndroidViewModel(application) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val repository: AppRepository = application.getRepository
    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

    fun createLocalBackup() {
        viewModelScope.launch(ioDispatcher) {
            selectedApps?.let {
                val backupWorkerHelper = BackupWorkerHelper(selectedApps, workManager)
                backupWorkerHelper.startBackupWorker()
            }
        }
    }

    suspend fun getCurrentApp(packageName: String) =
        repository.getAppByPackageName(packageName)
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