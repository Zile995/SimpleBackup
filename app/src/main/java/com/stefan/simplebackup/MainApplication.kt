package com.stefan.simplebackup

import android.app.Application
import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper.initPreferences
import com.stefan.simplebackup.utils.work.backup.ROOT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob


/**
 * - Our main [Application] based class
 * - Contains read-only properties
 * - Mainly, objects will be created when they are called
 */
class MainApplication : Application() {
    /**
     * - Main database scope using the [SupervisorJob].
     * - All other child scopes will be canceled if this scope is canceled
     * - If one child coroutine fails, others will not
     */
    private val applicationScope = CoroutineScope(SupervisorJob())

    /**
     * - Reference to singleton object of [AppDatabase] class
     * - Used to create, open, update Room SQL database
     * - It is controlled by [AppRepository] which uses interface [AppDao] exposed methods
     * - It will be initialised lazily, on the first call
     */
    private val database by lazy {
        AppDatabase.getInstance(
            this,
            applicationScope,
            getAppManager
        )
    }

    /**
     * - App Builder class reference
     * - Used to create [AppData] class objects in ViewModel or Database Callback
     * - It will be initialised lazily, on the first call
     */
    private val appManager: AppManager by lazy { AppManager(this) }


    /**
     * - Used to update Room [AppDatabase] content by using [AppDao] methods
     */
    private val repository by lazy { AppRepository(database.appDao()) }

    /**
     * - Get [AppRepository] reference
     */
    val getRepository get() = repository

    /**
     * - Get [AppManager] reference
     */
    val getAppManager get() = appManager

    override fun onCreate() {
        super.onCreate()
        initPreferences()
        setMainBackupDir()
        Log.d("MainApplication", "Started creating database")
        database
    }

    companion object {
        /**
         * - Used to get our main backup dir path
         */
        lateinit var mainBackupDirPath: String

        private fun Context.setMainBackupDir() {
            val externalFilesDir = this.getExternalFilesDir(null)?.absolutePath ?: ""
            mainBackupDirPath =
                externalFilesDir.substring(0, externalFilesDir.indexOf("Android")) + ROOT
        }
    }

}