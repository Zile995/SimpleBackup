package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.EventKind
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.findJsonFiles
import com.stefan.simplebackup.utils.file.JsonUtil.deserializeApp
import com.stefan.simplebackup.utils.file.asRecursiveFileWatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class BackupFilesObserver(private val rootDirPath: String) : FileEventObserver<AppData> {

    override val fileEventObserver by lazy {
        File(rootDirPath).asRecursiveFileWatcher().processFileEvents()
    }

    override suspend fun observeFileEvents(observableList: MutableStateFlow<MutableList<AppData>>) =
        coroutineScope {
            fileEventObserver.collect { fileEvent ->
                val list = mutableListOf<AppData>()
                list.addAll(observableList.value)
                fileEvent.apply {
                    when (kind) {
                        EventKind.CREATED -> {
                            Log.d("BackupFilesObserver", "On create $file")
                            val jsonDirPath = if (file.isDirectory) file.absolutePath else
                                file.parent!!
                            val jsonFile = findJsonFiles(jsonDirPath)
                            jsonFile.collectLatest {
                                val modifiedApp = deserializeApp(it)
                                modifiedApp?.apply {
                                    if (!list.contains(this)) {
                                        Log.d(
                                            "BackupFilesObserver",
                                            "Adding ${modifiedApp.name}"
                                        )
                                        list.add(this)
                                    }
                                }
                            }
                        }
                        EventKind.DELETED -> {
                            Log.d("BackupFilesObserver", "On delete $file")
                            if (file.absolutePath == FileUtil.localDirPath) {
                                list.clear()
                                return@apply
                            }
                            val deletedIndex = list.indexOfFirst { app ->
                                app.name == file.nameWithoutExtension
                                        || app.packageName == file.nameWithoutExtension
                            }
                            if (deletedIndex < 0) return@apply
                            Log.d(
                                "BackupFilesObserver",
                                "Deleting ${list[deletedIndex].name}"
                            )
                            list.removeAt(deletedIndex)
                        }
                        EventKind.MODIFIED -> {
                            Log.d("BackupFilesObserver", "On modified $file")
                            if (file.isFile && file.extension == "json") {
                                val modifiedApp = deserializeApp(file)
                                modifiedApp?.apply {
                                    list.remove(modifiedApp)
                                    Log.d(
                                        "BackupFilesObserver",
                                        "Adding ${modifiedApp.name}"
                                    )
                                    list.add(modifiedApp)
                                }
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

    override suspend fun refreshFileList(
        observableList: MutableStateFlow<MutableList<AppData>>,
        filter: (AppData) -> Boolean
    ) =
        coroutineScope {
            Log.d("BackupFilesObserver", "Refreshing the backup list")
            val backupList = mutableListOf<AppData>()
            findJsonFiles(rootDirPath).collect { jsonFile ->
                val app = deserializeApp(jsonFile)
                app?.let {
                    backupList.add(it)
                }
            }
            observableList.value = backupList.filter(filter).toMutableList()
        }
}