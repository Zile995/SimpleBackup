package com.stefan.simplebackup.database

import android.app.Application
import com.stefan.simplebackup.data.AppBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class DatabaseApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    /**
     * - App Builder class reference
     * - Used to create [Application] objects in ViewModel or Database Callback
     */
    private val appBuilder: AppBuilder by lazy { AppBuilder(this) }

    /**
     * - Reference to singleton object of [AppDatabase] class
     * - Used to create, open, update Room SQL database
     * - It is controlled by [AppRepository] which uses [AppDao] exposed interface methods
     */
    private val database by lazy { AppDatabase.getDbInstance(this, applicationScope, getAppBuilder) }

    /**
     * - Used to update Room [AppDatabase] content by using [AppDao] methods
     */
    private val repository by lazy { AppRepository(database.appDao()) }

    /**
     * - Get [AppRepository] reference
     */
    val getRepository get() = repository

    /**
     * - Get [AppBuilder] reference
     */
    val getAppBuilder get() = appBuilder
}