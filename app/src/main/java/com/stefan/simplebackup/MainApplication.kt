package com.stefan.simplebackup

import android.app.Application
import android.content.Context
import com.stefan.simplebackup.utils.PreferenceHelper.initPreferences
import kotlinx.coroutines.MainScope

const val MAIN_BACKUP_DIR_NAME: String = "SimpleBackup"

/**
 * - Main [Application] based class
 */
class MainApplication : Application() {

    val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        initPreferences()
        setMainBackupDir()
    }

    companion object {
        /**
         * - Get main backup dir path
         */
        lateinit var mainBackupDirPath: String
            private set

        private fun Context.setMainBackupDir() {
            val externalFilesDir = this.getExternalFilesDir(null)?.absolutePath ?: ""
            mainBackupDirPath = externalFilesDir.substring(
                0, externalFilesDir.indexOf("Android")
            ) + MAIN_BACKUP_DIR_NAME
        }
    }
}