package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.FileUtil.moveFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val ROOT: String = "SimpleBackup/local"
const val TEMP: String = "SimpleBackup/temp"

@Suppress("BlockingMethodInNonBlockingContext")
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

    fun getTempDirPath(app: AppData): String {
        val tempDirPath = mainBackupDirPath.replace(ROOT, TEMP)
        return "$tempDirPath/${app.packageName}"
    }

    suspend fun moveBackup(app: AppData) {
        val sourceFile = File(getTempDirPath(app))
        val targetFile = File(getBackupDirPath(app) + "/")
        sourceFile.moveFile(targetFile)
    }

    suspend fun serializeApp(app: AppData) {
        app.isLocal = true
        JsonUtil.serializeApp(app, getTempDirPath(app))
    }

    fun setBackupTime(app: AppData) {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app.date = time.format(Date())
    }
}