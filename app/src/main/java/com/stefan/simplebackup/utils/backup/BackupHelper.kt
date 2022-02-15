package com.stefan.simplebackup.utils.backup

import android.icu.text.SimpleDateFormat
import androidx.work.Data
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import java.util.*

class BackupHelper(private val appList: MutableList<AppData>, private val mainDirPath: String) {

    constructor(app: AppData, mainDirPath: String) : this(mutableListOf(app), mainDirPath)

    private val backupDirPaths = arrayListOf<String>()

    suspend fun createInputData(): Data {
        val builder = Data.Builder()
        prepare()
        builder.putStringArray("BACKUP_PATHS", backupDirPaths.toTypedArray())
        return builder.build()
    }

    private suspend fun prepare() {
        createMainDir()
        appList.forEach { app ->
            val backupDirPath = mainDirPath + "/" + app.getPackageName()
            addBackupDirPath(backupDirPath)
            createAppBackupDir(backupDirPath)
            setBackupTime(app)
            serializeApp(app, backupDirPath)
        }
    }

    private fun addBackupDirPath(backupDirPath: String) {
        backupDirPaths.add(backupDirPath)
    }

    private suspend fun serializeApp(app: AppData, backupDirPath: String) {
        FileUtil.serializeApp(app, backupDirPath)
    }

    private fun setBackupTime(app: AppData) {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app.setDate(time.format(Date()))
    }

    private suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainDirPath)
            createFile("${mainDirPath}/.nomedia")
        }
    }

    private suspend fun createAppBackupDir(backupDirPath: String) {
        FileUtil.createDirectory(backupDirPath)
    }

}