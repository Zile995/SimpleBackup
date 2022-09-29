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
import com.stefan.simplebackup.utils.file.EventKind
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import com.stefan.simplebackup.utils.file.asRecursiveFileWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class LocalViewModel(
    application: MainApplication,
    private val repository: AppRepository
) :
    BaseViewModel() {

    private val workManager = WorkManager.getInstance(application)

    init {
        Log.d("ViewModel", "LocalViewModel created")
    }

    init {
        viewModelScope.launch {
            fetchPackageList()
            val fileWatcher = File(backupDirPath).asRecursiveFileWatcher()
            fileWatcher.processFileEvents().collect { fileEvent ->
                val list = mutableListOf<AppData>()
                list.addAll(_observableList.value)
                when (fileEvent.kind) {
                    EventKind.MODIFIED -> {
                        val jsonFile = findJsonFiles(fileEvent.file.absolutePath)
                        jsonFile.collect {
                            val modifiedApp = deserializeApp(it)
                            modifiedApp?.apply {
                                val oldIndex = _observableList.value.indexOfFirst { oldData ->
                                    oldData.packageName == packageName
                                }
                                list.removeAt(oldIndex)
                                list.add(oldIndex, modifiedApp)
                            }
                        }
                    }
                    EventKind.CREATED -> {
                        val jsonFile = findJsonFiles(fileEvent.file.absolutePath)
                        jsonFile.collect {
                            val modifiedApp = deserializeApp(it)
                            modifiedApp?.apply {
                                list.add(this)
                            }
                        }
                    }
                    EventKind.DELETED -> {
                        Log.d("ViewModel", "On delete if ${fileEvent.file.name}")
                        val deletedApp = list.firstOrNull { appData ->
                            appData.packageName == fileEvent.file.name
                        }
                        list.remove(deletedApp)
                    }
                }
                _observableList.value = list
            }
        }
    }

    private suspend fun fetchPackageList() = coroutineScope {
        val backupList = mutableListOf<AppData>()
        findJsonFiles(backupDirPath).collect { jsonFile ->
            val app = deserializeApp(jsonFile)
            app?.let {
                backupList.add(it)
            }
        }
        _observableList.value = backupList
        delay(400)
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