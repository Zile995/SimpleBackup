package com.stefan.simplebackup.database

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class DatabaseApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    private val database by lazy { AppDatabase.getDbInstance(this, applicationScope) }
    private val repository by lazy { AppRepository(database.appDao()) }

    val getRepository get() = repository
}