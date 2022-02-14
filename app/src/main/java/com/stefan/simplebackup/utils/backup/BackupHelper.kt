package com.stefan.simplebackup.utils.backup

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.work.Data
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import java.util.*
import kotlin.collections.ArrayList

class BackupHelper(private var app: AppData?, private val mainDirPath: String) {

    private var backupDirPath = mainDirPath + "/" + app?.getPackageName()
    val getBackupDirPath get() = backupDirPath

    fun createInputData(backupDirPaths: ArrayList<String>): Data {
        val builder = Data.Builder()
        builder.putStringArray("BACKUP_PATHS", backupDirPaths.toTypedArray())
        return builder.build()
    }

    suspend fun prepare() {
        app?.let {
            setBackupTime()
            createMainDir()
            createAppBackupDir()
            serializeApp()
        }
    }

    fun setAnApp(newApp: AppData) {
        app = newApp
        backupDirPath = getNewDirPath(newApp)
    }

    private fun getNewDirPath(newApp: AppData): String {
        return mainDirPath + "/" + newApp.getPackageName()
    }

    private suspend fun serializeApp() {
        app?.let { backupApp ->
            FileUtil.serializeApp(backupApp, backupDirPath)
        }
    }

    private fun setBackupTime() {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        Log.d("ViewModel", "Setting the backup time")
        app?.setDate(time.format(Date()))
    }

    private suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainDirPath)
            createFile("${mainDirPath}/.nomedia")
        }
    }

    private suspend fun createAppBackupDir() {
        app?.let {
            FileUtil.createDirectory(backupDirPath)
        }
    }

}