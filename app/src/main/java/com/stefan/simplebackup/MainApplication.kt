package com.stefan.simplebackup

import android.app.Application
import android.os.Environment
import com.stefan.simplebackup.utils.PreferenceHelper.initPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

const val MAIN_BACKUP_DIR_NAME: String = "SimpleBackup"

/**
 * - Main [Application] based class
 */
class MainApplication : Application() {

    /**
     * - Main application CoroutineScope. It has [SupervisorJob] and [Dispatchers.Main] context elements
     * - It can be used for database operations
     * - This scope will be canceled when the app process dies.
     */
    val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        initPreferences()
    }

    companion object {
        /**
         * - Main backup dir path
         */
        var mainBackupDirPath: String =
            Environment.getExternalStorageDirectory().absolutePath + "/$MAIN_BACKUP_DIR_NAME"
            private set

    }
}