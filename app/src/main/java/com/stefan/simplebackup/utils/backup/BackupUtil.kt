package com.stefan.simplebackup.utils.backup

import android.util.Log
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.io.File

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(inputDataPath: String) {

    private var app: AppData? = null
    val getApp get() = app

    private val inputData = File(inputDataPath)
    private val zipUtil by lazy { ZipUtil(inputData.absolutePath) }

    init {
        Log.d("BackupUtil", "Created BackupUtil")
        runBlocking {
            deserializeApp()
        }
    }

    suspend fun backup() {
        app?.let {
            zipUtil.zipApk(getApkList(), inputData.absolutePath)
        }
    }

    private suspend fun deserializeApp() {
        findSerializedApp().collect { jsonFile ->
            FileUtil.deserializeApp(jsonFile).collect { jsonApp ->
                Log.d("BackupUtil", "I got the ${jsonApp.getName()}")
                app = jsonApp
            }
        }
    }

    private fun findSerializedApp() = flow<File> {
        Log.d("BackupUtil", "Finding the json file")
        inputData.listFiles()?.filter { jsonFile ->
            jsonFile.isFile && jsonFile.extension == "json"
        }?.map { jsonFile ->
            emit(jsonFile)
        }
    }

    private fun getApkList(): MutableList<File> {
        val apkList = mutableListOf<File>()
        app?.let { app ->
            Log.d("BackupUtil", "${app.getName()} dir: ${app.getApkDir()}")
            val dir = File(app.getApkDir())
            dir.walkTopDown().filter {
                it.extension == "apk"
            }.forEach { apk ->
                apkList.add(apk)
            }
        }
        Log.d("BackupUtil", "Got the apk list: ${apkList.map { it.name }}")
        return apkList
    }
}