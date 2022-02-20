package com.stefan.simplebackup.database

import android.app.Application
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.backup.ROOT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * - Our main [Application] based class
 * - Contains read-only properties
 * - Mainly, objects will be created when they are called
 */
class DatabaseApplication : Application() {
    /**
     * - Main database scope using the [SupervisorJob].
     * - All other child scopes will be canceled if this scope is canceled
     * - If one child coroutine fails, others will not
     */
    private val applicationScope = CoroutineScope(SupervisorJob())

    /**
     * - Used to get external file dir path
     * - It is usually Android/data/packageName directory
     */
    private val externalFilesDir: String by lazy {
        getExternalFilesDir(null)?.absolutePath ?: ""
    }

    /**
     * - Used to get our main backup dir path
     */
    private val mainBackupDirPath: String get() {
        return externalFilesDir.let { path ->
            path.substring(0, path.indexOf("Android")) + ROOT
        }
    }

    /**
     * - App Builder class reference
     * - Used to create [AppData] class objects in ViewModel or Database Callback
     * - It will be initialised lazily, on the first call
     */
    private val appManager: AppManager by lazy { AppManager(this) }

    /**
     * - Reference to singleton object of [AppDatabase] class
     * - Used to create, open, update Room SQL database
     * - It is controlled by [AppRepository] which uses interface [AppDao] exposed methods
     * - It will be initialised lazily, on the first call
     */
    private val database by lazy {
        AppDatabase.getDbInstance(
            this,
            applicationScope,
            getAppManager
        )
    }

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

    /**
     * - Get main backup dir path
     */
    val getMainBackupDirPath get() = mainBackupDirPath

    override fun onCreate() {
        super.onCreate()
        database
    }

}