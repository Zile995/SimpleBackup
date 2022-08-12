package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.MainApplication.Companion.backupDirPath
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import com.stefan.simplebackup.utils.extensions.launchWithLogging
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import kotlinx.coroutines.*

class LocalViewModel(application: MainApplication, private val repository: AppRepository) :
    BaseViewModel() {

    private val workManager = WorkManager.getInstance(application)
    //private val fileWatcher = File(mainBackupDirPath).asFileWatcher()

    init {
        Log.d("ViewModel", "LocalViewModel created")
        viewModelScope.launchWithLogging(CoroutineName("LoadLocalList")) {
            loadList {
                repository.localApps
            }
        }
    }

    suspend fun startPackagePolling() {
        withContext(Dispatchers.Default) {
            launch {
                while (true) {
                    findJsonFiles(backupDirPath).collect { jsonFile ->
                        val app = deserializeApp(jsonFile)
                        app?.let {
                            repository.insertAppData(it)
                        }
                    }
                    delay(1_500)
                }
            }
        }
    }

    fun startRestoreWorker(uid: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val workerHelper = WorkerHelper(uid, workManager)
            workerHelper.beginUniqueWork<MainWorker>(shouldBackup = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "LocalViewModel cleared")
    }
}