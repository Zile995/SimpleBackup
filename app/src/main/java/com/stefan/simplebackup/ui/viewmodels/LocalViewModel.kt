package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.MainApplication.Companion.backupDirPath
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class LocalViewModel(
    application: MainApplication,
    private val repository: AppRepository
) : BaseViewModel(),
    FileEventObserver<AppData> by BackupFilesObserver(rootDirPath = backupDirPath) {

    private val workManager = WorkManager.getInstance(application)

    init {
        Log.d("ViewModel", "LocalViewModel created")
        refreshBackupList()
        observeFilesEvents(
            scope = viewModelScope,
            observable = _observableList
        )
    }

    fun refreshBackupList() =
        viewModelScope.launch {
            Log.d("ViewModel", "Refreshing the backup list")
            val backupList = mutableListOf<AppData>()
            val time = measureTimeMillis {
                findJsonFiles(backupDirPath).collect { jsonFile ->
                    val app = deserializeApp(jsonFile)
                    app?.let {
                        backupList.add(it)
                    }
                }
                _observableList.value = backupList
            }
            if (time < 400L && spinner.value) delay(400 - time)
            _spinner.value = false
        }

    fun startRestoreWorker(packageName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val workerHelper = WorkerHelper(packageName = packageName, workManager)
            workerHelper.beginUniqueWork<MainWorker>(shouldBackup = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "LocalViewModel cleared")
    }
}