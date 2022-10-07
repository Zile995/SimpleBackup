package com.stefan.simplebackup.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.showToast
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

private const val DATABASE_NAME = "app_database"

/**
 * Singleton AppData Database class
 */
@Database(entities = [AppData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope,
    ) : Callback() {

        private var mainJob: Job? = null
        private val ioDispatcher = Dispatchers.IO
        private val appDao by lazy { INSTANCE?.appDao() }

        private suspend fun insertAll() {
            try {
                val appManager = AppManager(context)
                val time = measureTimeMillis {
                    appManager.apply {
                        updateSequenceNumber()
                        dataBuilder().collect { app ->
                            println("Inserting: ${app.name}")
                            appDao?.insert(app)
                        }
                    }
                }
                PreferenceHelper.setDatabaseCreated(true)
                Log.d("AppDatabase", "Insert time: $time")
            } catch (e: SQLiteException) {
                Log.e("AppDatabase", "Error: $e ${e.message}")
                context.showToast("Database error, can't insert new items: $e ${e.message}")
            }
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            mainJob = scope.launch(ioDispatcher) {
                Log.d("AppDatabase", "Creating database")
                insertAll()
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch(ioDispatcher) {
                mainJob?.join()
                if (!PreferenceHelper.isDatabaseCreated) {
                    Log.d("AppDatabase", "Updating database again")
                    insertAll()
                    mainJob = null
                }
            }
        }
    }

    companion object {

        /**
         * - Private singleton [AppDatabase] class instance
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * - Create or get reference to singleton object of [AppDatabase] class
         * - Used to create, open, update Room SQL database
         * - It is controlled by [AppRepository] which uses [AppDao] interface exposed methods
         */
        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, scope).also { appDatabase ->
                    INSTANCE = appDatabase
                }
            }

        /**
         * - This function should be used for creating [AppDatabase] class instance.
         * - Main database coroutine scope should use [SupervisorJob] job with [Dispatchers.IO] dispatcher.
         * - All other child coroutines will be canceled if this scope is canceled, if one child coroutine
         *   fails, others will not
         */
        private fun buildDatabase(
            context: Context,
            scope: CoroutineScope
        ) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, DATABASE_NAME
            )
                .addCallback(AppDatabaseCallback(context, scope))
                .build()
    }
}