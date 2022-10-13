package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFilesRecursively
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        recursiveFileWatcher.fileEvent.collectLatest { fileEvent ->
            fileEvent.apply {
                Log.d("BackupFilesObserver", "Event file $file ${file.isDirectory} $kind")
                observableList.updateCurrentList { currentList ->
                    when (kind) {
                        EventKind.CREATED -> {
                            Log.d("BackupFilesObserver", "On create $file")
                            val jsonFile = when {
                                file.extension == JSON_FILE_EXTENSION
                                        && !file.name.endsWith(".$JSON_FILE_EXTENSION") -> file
                                file.extension != ZIP_FILE_EXTENSION ->
                                    FileUtil.findFirstJsonInDir(file.absolutePath)
                                else -> null
                            }
                            jsonFile?.let {
                                val createdApp = deserializeApp(jsonFile)
                                Log.d(
                                    "BackupFilesObserver",
                                    "Created app = ${createdApp?.name}"
                                )
                                createdApp?.apply {
                                    if (!currentList.contains(this)) {
                                        Log.d(
                                            "BackupFilesObserver",
                                            "Adding created ${createdApp.name}"
                                        )
                                        currentList.add(this)
                                    }
                                }
                            }
                        }
                        EventKind.DELETED -> {
                            Log.d("BackupFilesObserver", "On delete $file")
                            val deletedApp = currentList.firstOrNull { app ->
                                (app.name == file.nameWithoutExtension && file.extension == JSON_FILE_EXTENSION)
                                        || app.packageName == file.name
                            }
                            Log.d(
                                "BackupFilesObserver",
                                "Deleted app = ${deletedApp?.name}"
                            )
                            currentList.remove(deletedApp)
                        }
                        EventKind.MODIFIED -> {
                            if (file.extension == JSON_FILE_EXTENSION && file.isFile) {
                                Log.d("BackupFilesObserver", "On modified $file")
                                val modifiedApp = deserializeApp(file)
                                Log.d(
                                    "BackupFilesObserver",
                                    "Modified app = ${modifiedApp?.name}"
                                )
                                modifiedApp?.apply {
                                    Log.d(
                                        "BackupFilesObserver",
                                        "Adding modified ${modifiedApp.name}"
                                    )
                                    val indexOfDeleted =
                                        currentList.indexOfFirst { it.packageName == modifiedApp.packageName }
                                    if (indexOfDeleted < 0) return@updateCurrentList
                                    currentList[indexOfDeleted] = modifiedApp
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private inline fun <T> MutableStateFlow<MutableList<T>>.updateCurrentList(
        action: (MutableList<T>) -> Unit
    ) {
        val newValue = value.toMutableList()
        action(newValue)
        value = newValue
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