package com.stefan.simplebackup

import android.app.Application
import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.utils.PreferenceHelper.initPreferences
import com.stefan.simplebackup.utils.file.BACKUP_DIR_PATH
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * - Main [Application] based class
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initPreferences()
        setMainBackupDir()
    }

    companion object {
        /**
         * - Get main backup dir path
         */
        lateinit var backupDirPath: String
            private set

        private fun Context.setMainBackupDir() {
            val externalFilesDir = this.getExternalFilesDir(null)?.absolutePath ?: ""
            backupDirPath =
                externalFilesDir.substring(0, externalFilesDir.indexOf("Android")) + BACKUP_DIR_PATH
        }
    }
}