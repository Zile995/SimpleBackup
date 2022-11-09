package com.stefan.simplebackup

import android.app.Application
import android.os.Environment
import com.stefan.simplebackup.utils.PreferenceHelper.initPreferences
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

const val MAIN_BACKUP_DIR_NAME: String = "SimpleBackup"

/**
 * - Main [Application] based class
 */
class MainApplication : Application() {

    init {
        // Set libsu main shell settings before the main shell can be created
        val builder = Shell.Builder.create()
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            builder.apply {
                setFlags(Shell.FLAG_MOUNT_MASTER)
                setTimeout(10)
            }
        )
    }

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