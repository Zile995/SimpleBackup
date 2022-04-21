package com.stefan.simplebackup.utils.backup

import android.content.Context
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.FileUtil
import com.stefan.simplebackup.utils.main.JsonUtil
import java.text.SimpleDateFormat
import java.util.*

open class BackupHelper(context: Context) {

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
        return "$mainBackupDirPath/${app.packageName}"
    }

    protected suspend fun serializeApp(app: AppData) {
        JsonUtil.serializeApp(app, getBackupDirPath(app))
    }

//    protected suspend fun getSerializedApp(packageName: String): Flow<AppData?> = flow {
//        FileUtil.findJsonFile("$mainBackupDirPath/$packageName")
//            .collectLatest { jsonFile ->
//                jsonFile?.let {
//                    JsonUtil.deserializeApp(it).collect { app ->
//                        emit(app)
//                    }
//                }
//            }
//    }

    protected fun setBackupTime(app: AppData) {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        app.date = time.format(Date())
    }
}