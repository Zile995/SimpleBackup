package com.stefan.simplebackup.viewmodel

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.backup.BackupHelper
import com.stefan.simplebackup.workers.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel(
    private val app: AppData?,
    application: DatabaseApplication
) : AndroidViewModel(application) {

    val selectedApp get() = app
    private val workManager = WorkManager.getInstance(application)

    private val backupDirPath by lazy { application.getMainBackupDirPath + "/" + app?.getPackageName() }
    private val backupHelper by lazy { BackupHelper(app, application.getMainBackupDirPath) }

    init {
        Log.d("ViewModel", "BackupViewModel created")
    }

    fun createLocalBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            app?.let {
                val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                    .setInputData(createInputData())
                    .build()
                workManager.enqueue(backupRequest)
            }
        }
    }

    private suspend fun createInputData(): Data {
        val builder = Data.Builder()
        backupHelper.prepare()
        builder.putString("KEY_BACKUP_DATA_PATH", backupDirPath)
        return builder.build()
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