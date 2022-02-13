package com.stefan.simplebackup.utils.backup

import android.icu.text.SimpleDateFormat
import android.util.Log
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import java.util.*

class BackupHelper(private val app: AppData?, private val mainDirPath: String) {

    private val backupDirPath by lazy { mainDirPath + "/" + app?.getPackageName() }

    suspend fun prepare() {
        app?.let {
            setBackupTime()
            createMainDir()
            createAppBackupDir()
            serializeApp()
        }
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