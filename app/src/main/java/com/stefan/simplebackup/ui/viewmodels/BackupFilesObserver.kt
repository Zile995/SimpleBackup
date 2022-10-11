package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.EventKind
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFilesRecursively
import com.stefan.simplebackup.utils.file.JSON_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import com.stefan.simplebackup.utils.file.asRecursiveFileWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File

class BackupFilesObserver(
    private val rootDirPath: String,
    private val scope: CoroutineScope,
    private var observableList: MutableStateFlow<MutableList<AppData>>
) {

    private val recursiveFileWatcher by lazy {
        File(rootDirPath).asRecursiveFileWatcher(scope)
    }

    init {
        scope.launch {
            val rootDir = File(rootDirPath)
            if (!rootDir.exists()) FileUtil.createDirectory(rootDirPath)
        }
    }

    fun observeBackupFiles() = scope.launch {
        recursiveFileWatcher.fileEvent.distinctUntilChanged().collect { fileEvent ->
            val list = mutableListOf<AppData>()
            list.addAll(observableList.value)
            fileEvent.apply {
                when (kind) {
                    EventKind.CREATED -> {
                        Log.d("BackupFilesObserver", "On create $file")
                        val jsonFile = if (file.isDirectory) {
                            FileUtil.findFirstJsonInDir(file.absolutePath)
                        } else file
                        jsonFile?.let {
                            val createdApp = deserializeApp(jsonFile)
                            Log.d("BackupFilesObserver", "Created app = ${createdApp?.name}")
                            createdApp?.apply {
                                if (!list.contains(this)) {
                                    Log.d(
                                        "BackupFilesObserver",
                                        "Adding created ${createdApp.name}"
                                    )
                                    list.add(this)
                                }
                            }
                        }
                    }
                    EventKind.DELETED -> {
                        Log.d("BackupFilesObserver", "On delete $file")
                        val deletedIndex = list.indexOfFirst { app ->
                            (app.name == file.nameWithoutExtension && file.extension == JSON_FILE_EXTENSION)
                                    || app.packageName == file.name
                        }
                        if (deletedIndex < 0) return@apply
                        Log.d("BackupFilesObserver", "Deleted app = ${list[deletedIndex].name}")
                        list.removeAt(deletedIndex)
                    }
                    EventKind.MODIFIED -> {
                        Log.d("BackupFilesObserver", "On modified $file")
                        val modifiedApp = deserializeApp(file)
                        Log.d("BackupFilesObserver", "Modified app = ${modifiedApp?.name}")
                        modifiedApp?.apply {
                            list.remove(modifiedApp)
                            Log.d(
                                "BackupFilesObserver", "Adding modified ${modifiedApp.name}"
                            )
                            list.add(modifiedApp)
                        }
                    }
                    else -> {
                        return@apply
                    }
                }
                observableList.value = list
            }
        }
    }

    fun refreshBackupFileList(predicate: (AppData) -> Boolean = { it.isLocal }) = scope.launch {
        Log.d("BackupFilesObserver", "Refreshing the backup list")
        val backupList = mutableListOf<AppData>()
        findJsonFilesRecursively(jsonDirPath = rootDirPath).collect { jsonFile ->
            val app = deserializeApp(jsonFile)
            app?.let {
                if (predicate(it)) {
                    backupList.add(it)
                }
            }
        }
        observableList.value = backupList
    }
}