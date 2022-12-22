package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.work.FileUtil
import com.stefan.simplebackup.utils.work.JSON_FILE_EXTENSION
import com.stefan.simplebackup.utils.work.JsonUtil.deserializeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.io.File

class BackupFilesObserver(
    private val rootDirPath: String,
    private val scope: CoroutineScope,
    private val appRepository: AppRepository
) {

    private val ioDispatcher = Dispatchers.IO

    private val recursiveFileWatcher by lazy {
        File(rootDirPath).asRecursiveFileWatcher(scope)
    }

    fun refreshBackupList() = scope.launch(ioDispatcher) {
        Log.d("BackupFilesObserver", "Refreshing backup list")
        val newBackupList = mutableListOf<AppData>()
        FileUtil.findJsonFiles(dirPath = rootDirPath).collect { jsonFile ->
            if (jsonFile.parentFile?.parentFile?.absolutePath != rootDirPath) return@collect
            val app = deserializeApp(jsonFile)
            app?.let {
                if (jsonFile.parentFile?.isDirectory == true
                    && jsonFile.parentFile?.name == it.packageName
                    && it.isLocal
                ) {
                    newBackupList.add(it)
                }
            }
        }
        submitBackupList(newBackupList.sortedBy { it.name })
    }

    fun observeBackupFiles() = scope.launch(ioDispatcher) {
        recursiveFileWatcher.fileEvent.collect { event ->
            Log.d("BackupFilesObserver", "$event.kind: ${event.file.absolutePath}")
            when (event.kind) {
                EventKind.CREATED -> {
                    onCreatedEvent(event.file)
                }
                EventKind.DELETED -> {
                    onDeletedEvent(event.file)
                }
                EventKind.MODIFIED -> {
                    onModifiedEvent(event.file)
                }
            }
        }
    }

    private fun takeCurrentList() = appRepository.localApps.take(1)

    private suspend fun submitBackupList(newList: List<AppData>) = coroutineScope {
        takeCurrentList().collect { currentList ->
            if (currentList != newList) {
                Log.d("BackupFilesObserver", "Submitting, the new backup list is different")
                launch {
                    insertNewBackups(newList)
                }
                removeDeletedBackups()
            }
        }
    }

    private suspend fun insertNewBackups(newList: List<AppData>) = coroutineScope {
        newList.forEach {
            launch {
                appRepository.insert(it)
            }
        }
    }

    private suspend fun removeDeletedBackups() {
        takeCurrentList().collect { currentList ->
            currentList.forEach { app ->
                val backupDir = FileUtil.getBackupDirPath(app)
                if (FileUtil.getJsonInDir(backupDir)?.exists() != true)
                    appRepository.deleteLocal(app.packageName)
            }
        }
    }


    private suspend fun onCreatedEvent(file: File) {
        val jsonFile = when {
            file.isFile && file.extension == JSON_FILE_EXTENSION -> file
            file.isDirectory && file.parentFile?.absolutePath == rootDirPath -> {
                FileUtil.getJsonInDir(file.absolutePath)
            }
            else -> null
        }
        jsonFile?.let {
            val createdApp = deserializeApp(it)
            if (jsonFile.parentFile?.name == createdApp?.packageName) {
                createdApp?.apply {
                    Log.d("BackupFilesObserver", "Creating app $name")
                    appRepository.insertAppData(createdApp)
                }
            }
        }
    }

    private suspend fun onDeletedEvent(file: File) {
        takeCurrentList().collect { currentList ->
            currentList.firstOrNull { app ->
                (app.name == file.nameWithoutExtension
                        && app.packageName == file.parentFile?.name
                        && file.extension == JSON_FILE_EXTENSION)
                        || (app.packageName == file.name && file.parentFile?.name != file.name)
            }?.apply {
                Log.d("BackupFilesObserver", "Removing deleted $name")
                appRepository.deleteLocal(packageName)
            }
        }
    }

    private suspend fun onModifiedEvent(file: File) {
        if (file.extension == JSON_FILE_EXTENSION && file.isFile) {
            val modifiedApp = deserializeApp(file)
            modifiedApp?.apply {
                if (file.parentFile?.name == packageName) {
                    Log.d("BackupFilesObserver", "Adding modified $name")
                    appRepository.insert(this)
                }
            }
        }
    }
}