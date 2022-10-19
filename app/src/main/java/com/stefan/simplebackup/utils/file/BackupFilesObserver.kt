package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.FileUtil.emitJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class BackupFilesObserver(
    private val rootDirPath: String,
    private val scope: CoroutineScope,
    private val observableList: MutableStateFlow<MutableList<AppData>>
) {

    private val recursiveFileWatcher by lazy {
        File(rootDirPath).asRecursiveFileWatcher(scope)
    }

    fun observeBackupFiles() = scope.launch {
        recursiveFileWatcher.fileEvent.collectLatest { fileEvent ->
            fileEvent.apply {
                Log.d("BackupFilesObserver", "$kind: ${file.absolutePath}")
                observableList.updateCurrentList { currentList ->
                    when (kind) {

                        EventKind.CREATED -> {
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
                                        if (!currentList.contains(this)) {
                                            Log.d("BackupFilesObserver", "Adding created $name")
                                            currentList.add(this)
                                        }
                                    }
                                }
                            }
                        }

                        EventKind.DELETED -> {
                            val deletedApp = currentList.firstOrNull { app ->
                                (app.name == file.nameWithoutExtension && app.packageName == file.parentFile?.name && file.extension == JSON_FILE_EXTENSION)
                                        || (app.packageName == file.name && file.parentFile?.name != file.name)
                            }
                            currentList.remove(deletedApp)
                            Log.d("BackupFilesObserver", "Removing deleted ${deletedApp?.name}")
                        }

                        EventKind.MODIFIED -> {
                            if (file.extension == JSON_FILE_EXTENSION && file.isFile) {
                                val modifiedApp = deserializeApp(file)
                                modifiedApp?.apply {
                                    if (file.parentFile?.name == packageName) {
                                        val indexOfDeleted =
                                            currentList.indexOfFirst { it.packageName == packageName }
                                        if (indexOfDeleted < 0) return@updateCurrentList
                                        Log.d("BackupFilesObserver", "Adding modified $name")
                                        currentList[indexOfDeleted] = this
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private inline fun MutableStateFlow<MutableList<AppData>>.updateCurrentList(
        action: (MutableList<AppData>) -> Unit
    ) {
        val newValue = value.toMutableList()
        action(newValue)
        value = newValue.sortedBy { it.name }.toMutableList()
    }

    fun refreshBackupFileList(predicate: (AppData) -> Boolean = { it.isLocal }) = scope.launch {
        Log.d("BackupFilesObserver", "Refreshing the backup list")
        val backupList = mutableListOf<AppData>()
        emitJsonFiles(jsonDirPath = rootDirPath).collect { jsonFile ->
            if (jsonFile.parentFile?.parentFile?.absolutePath != rootDirPath) return@collect
            val app = deserializeApp(jsonFile)
            app?.let {
                if (jsonFile.parentFile?.isDirectory == true && jsonFile.parentFile?.name == it.packageName) {
                    if (predicate(it)) {
                        backupList.add(it)
                    }
                }
            }
        }
        observableList.value = backupList.sortedBy { it.name }.toMutableList()
    }
}