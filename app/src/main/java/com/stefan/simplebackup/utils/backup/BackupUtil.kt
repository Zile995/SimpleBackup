package com.stefan.simplebackup.utils.backup

import android.util.Log
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
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
            val zipUtil = ZipUtil()
            deserializedApps.forEach { entry ->
                Log.d("BackupUtil", "Backing up ${entry.key.getName()}")
                getApkList(entry.key).collect { apkFiles ->
                    zipUtil.zipApk(apkFiles, entry.value)
                }
            }
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

    private fun getApkList(app: AppData) = flow {
        val apkList = mutableListOf<File>()
        Log.d("BackupUtil", "${app.getName()} apk dir: ${app.getApkDir()}")
        val dir = File(app.getApkDir())
        dir.walkTopDown().filter {
            it.extension == "apk"
        }.forEach { apk ->
            apkList.add(apk)
        }
        Log.d("BackupUtil", "Got the apk list for ${app.getName()}: ${apkList.map { it.name }}")
        emit(apkList)
    }
}