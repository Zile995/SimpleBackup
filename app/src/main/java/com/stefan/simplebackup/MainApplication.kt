package com.stefan.simplebackup

import android.app.Application
import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.utils.PreferenceHelper.initPreferences
import com.stefan.simplebackup.utils.file.ROOT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * - Our main [Application] based class
 * - Contains read-only properties
 * - Mainly, objects will be created when they are called
 */
class MainApplication : Application() {

    /**
     * - Reference to singleton object of [AppDatabase] class
     * - Used to create, open, update Room SQL database
     * - It is controlled by [AppRepository] which uses interface [AppDao] exposed methods
     * - It will be initialised lazily, on the first call
     * - Main database coroutine scope is using [SupervisorJob] job with [Dispatchers.Default] dispatcher.
     * - All other child coroutines will be canceled if this scope is canceled
     * - If one child coroutine fails, others will not
     */
    val database by lazy {
        getDatabaseInstance(CoroutineScope(SupervisorJob()))
    }

    override fun onCreate() {
        super.onCreate()
        initPreferences()
        setMainBackupDir()
        Log.d("MainApplication", "Started creating database")
    }

    companion object {
        /**
         * - Used to get our main backup dir path
         */
        lateinit var mainBackupDirPath: String
            private set

        private fun Context.setMainBackupDir() {
            val externalFilesDir = this.getExternalFilesDir(null)?.absolutePath ?: ""
            mainBackupDirPath =
                externalFilesDir.substring(0, externalFilesDir.indexOf("Android")) + ROOT
        }

        fun Context.getDatabaseInstance(scope: CoroutineScope) =
            AppDatabase.getInstance(this, scope)
    }
}