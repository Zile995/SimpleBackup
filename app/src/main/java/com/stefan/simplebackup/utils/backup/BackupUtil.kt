package com.stefan.simplebackup.utils.backup

import android.util.Log
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.io.File

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(private val backupDirPaths: Array<String>) {

    private val deserializedApps = HashMap<AppData, String>()

    init {
        Log.d("BackupUtil", "Created BackupUtil")
        runBlocking {
            deserializeApps()
        }
    }

    suspend fun backup() {
        if (deserializedApps.isNotEmpty()) {
            zipApks()
        }
    }

    private suspend fun zipApks() {
        if (deserializedApps.isNotEmpty()) {
            val zipUtil = ZipUtil(deserializedApps)
            delay(3000)
            zipUtil.zipApk()
        }
    }

    private suspend fun deserializeApps() {
        findSerializedApps().collect { jsonFile ->
            FileUtil.deserializeApp(jsonFile).collect { jsonApp ->
                Log.d("BackupUtil", "I got the ${jsonApp.getName()}")
                deserializedApps[jsonApp] = jsonFile.absolutePath.substringBeforeLast("/")
            }
        }
    }

    private fun findSerializedApps() = flow<File> {
        Log.d("BackupUtil", "Finding the json file")
        backupDirPaths.forEach { backupDirPath ->
            File(backupDirPath).listFiles()?.filter { jsonFile ->
                jsonFile.isFile && jsonFile.extension == "json"
            }?.map { jsonFile ->
                emit(jsonFile)
            }
        }
    }
}