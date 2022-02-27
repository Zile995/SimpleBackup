package com.stefan.simplebackup.viewmodels

import android.util.Log
import androidx.lifecycle.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.backup.BackupWorkerHelper
import com.stefan.simplebackup.utils.backup.REQUEST_TAG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel(
    private val app: AppData?,
    private val application: DatabaseApplication
) : AndroidViewModel(application) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val workManager = WorkManager.getInstance(application)

    private var outputWorkInfo: LiveData<List<WorkInfo>> =
        workManager.getWorkInfosByTagLiveData(REQUEST_TAG)
    val getOutputWorkInfo get() = outputWorkInfo

    val selectedApp get() = app

    init {
        Log.d("ViewModel", "BackupViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(ioDispatcher) {
            app?.let { backupApp ->
                val backupWorkerHelper = BackupWorkerHelper(backupApp, workManager)
                backupWorkerHelper.startBackupWorker()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "BackupViewModel cleared")
    }
}

class BackupViewModelFactory(
    private val app: AppData?,
    private val application: DatabaseApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(app, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}