package com.stefan.simplebackup.utils.backup

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.util.Log
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.*

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(private var app: AppData?, context: Context) :
    StorageHelper(context) {

    private val appBackupDirPath by lazy { mainBackupDirPath + "/${app?.getName()}" }
    private var appJsonFile: File? = null
    private val zipUtil by lazy { ZipUtil(appBackupDirPath) }
    val getApp get() = app

    init {
        Log.d("BackupUtil", "Created BackupUtil")
        runBlocking {
            if (app == null)
                getApp()
        }
    }

    suspend fun backup() {
        app?.let { backupApp ->
            moveJsonFile()
            zipUtil.zipApk(getApkList(), backupApp.getDataDir())
        }
    }

    private suspend fun getApp() {
        findAppJson()
        appJsonFile?.let { jsonFile ->
            FileUtil.jsonToApp(jsonFile).collect { jsonApp ->
                Log.d("BackupUtil", "I got the ${jsonApp.getName()}")
                app = jsonApp
            }
        }
    }

    private fun findAppJson() {
        Log.d("BackupUtil", "Finding the json file")
        privateDir.listFiles()?.filter { jsonFile ->
            jsonFile.isFile && jsonFile.extension == "json"
        }?.map { jsonFile ->
            appJsonFile = jsonFile
        }
    }

    suspend fun prepare() {
        app?.let {
            setBackupTime()
            setAppBackupDataDir()
            createMainDir()
            createAppBackupDir()
            FileUtil.appToJsonFile(privateDir.absolutePath, it)
        }
    }

    private fun setBackupTime() {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app?.let { app ->
            Log.d("BackupUtil", "Setting the backup time")
            app.setDate(time.format(Date()))
        }
    }

    private fun setAppBackupDataDir() {
        app?.let { app ->
            Log.d("BackupUtil", "Setting the app backup dir $appBackupDirPath")
            app.setDataDir(appBackupDirPath)
        }
    }

    private suspend fun createAppBackupDir() {
        app?.let {
            FileUtil.createDirectory(appBackupDirPath)
        }
    }

    private suspend fun moveJsonFile() {
        appJsonFile?.let { jsonFile ->
            FileUtil.moveJsonFile(jsonFile, appBackupDirPath)
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