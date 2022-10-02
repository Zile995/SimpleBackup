package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.backupDirPath
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.EventKind
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.JsonUtil
import com.stefan.simplebackup.utils.file.asRecursiveFileWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class BackupFilesObserver(rootDirPath: String) : FileEventObserver<AppData> {

    override val fileEventObserver =
        File(rootDirPath).asRecursiveFileWatcher().processFileEvents()

    override fun observeFilesEvents(
        scope: CoroutineScope,
        observable: MutableStateFlow<MutableList<AppData>>
    ) {
        scope.launch {
            fileEventObserver.collect { fileEvent ->
                fileEvent.apply {
                    val list = mutableListOf<AppData>()
                    list.addAll(observable.value)
                    when (kind) {
                        EventKind.CREATED -> {
                            Log.d("BackupFilesObserver", "On create $file")
                            val jsonDirPath = if (file.isDirectory) file.absolutePath else
                                file.parent!!
                            val jsonFile = FileUtil.findJsonFiles(jsonDirPath)
                            jsonFile.collectLatest {
                                val modifiedApp = JsonUtil.deserializeApp(it)
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
                                val modifiedApp = JsonUtil.deserializeApp(file)
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
                    }
                    observable.value = list
                }
            }
        }
    }
}