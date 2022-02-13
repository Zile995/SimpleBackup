package com.stefan.simplebackup.database

import android.app.Application
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.utils.backup.ROOT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class DatabaseApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    private val internalStoragePath: String by lazy {
        getExternalFilesDir(null)?.absolutePath ?: ""
    }

    private val mainBackupDirPath: String get() {
        return internalStoragePath.let { path ->
            path.substring(0, path.indexOf("Android")) + ROOT
        }
    }

    /**
     * - App Builder class instance
     * - Used to create [Application] objects in ViewModel or Database Callback
     */
    private val appManager: AppManager by lazy { AppManager(this) }

    /**
     * - Reference to singleton object of [AppDatabase] class
     * - Used to create, open, update Room SQL database
     * - It is controlled by [AppRepository] which uses [AppDao] exposed interface methods
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