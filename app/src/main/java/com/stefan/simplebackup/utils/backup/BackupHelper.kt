package com.stefan.simplebackup.utils.backup

import android.content.Context
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

abstract class BackupHelper(context: Context) {

    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * - Used to get external file dir path
     * - It is usually Android/data/packageName directory
     */
    private val externalFilesDir: String by lazy {
        context.getExternalFilesDir(null)?.absolutePath ?: ""
    }

    /**
     * - Used to get our main backup dir path
     */
    private val mainBackupDirPath: String
        get() {
            return externalFilesDir.let { path ->
                path.substring(0, path.indexOf("Android")) + ROOT
            }
        }

    protected suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(mainBackupDirPath)
            createFile("${mainBackupDirPath}/.nomedia")
        }
    }

    protected suspend fun createAppBackupDir(backupDirPath: String) {
        FileUtil.createDirectory(backupDirPath)
    }

    protected fun getBackupDirPath(app: AppData): String {
        return mainBackupDirPath + "/" + app.getPackageName()
    }

    protected suspend fun serializeApp(app: AppData, backupDirPath: String) {
        FileUtil.serializeApp(app, backupDirPath)
    }

    protected fun setBackupTime(app: AppData) {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app.setDate(time.format(Date()))
    }
}