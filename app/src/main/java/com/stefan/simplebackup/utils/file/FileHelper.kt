package com.stefan.simplebackup.utils.file

import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.model.AppData
import java.text.SimpleDateFormat
import java.util.*

interface FileHelper {

    suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainBackupDirPath)
            createFile("${mainBackupDirPath}/.nomedia")
        }
    }

    suspend fun createAppBackupDir(backupDirPath: String) {
        FileUtil.createDirectory(backupDirPath)
    }

    fun getBackupDirPath(app: AppData): String {
        return "$mainBackupDirPath/${app.packageName}"
    }

    suspend fun serializeApp(app: AppData) {
        app.isLocal = true
        JsonUtil.serializeApp(app, getBackupDirPath(app))
    }

    fun setBackupTime(app: AppData) {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app.date = time.format(Date())
    }
}