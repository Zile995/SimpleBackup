package com.stefan.simplebackup.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
        private val appManager: AppManager
    ) :
        RoomDatabase.Callback() {

        private var mainJob: Job? = null
        private val appDao by lazy { INSTANCE?.appDao() }

        private suspend fun insertAll() {
            try {
                val time = measureTimeMillis {
                    appManager.apply {
                        updateSequenceNumber()
                        dataBuilder().collect { app ->
                            println("Inserting: ${app.name}")
                            appDao?.insert(app)
                        }
                        dataBuilder(true).collect { app ->
                            println("Inserting: ${app.name}")
                            appDao?.insert(app)
                        }
                    }
                }
                PreferenceHelper.setDatabaseCreated(true)
                Log.d("AppDatabase", "Load time: $time")
            } catch (e: SQLiteException) {
                Log.e("AppDatabase", "Error: ${e.localizedMessage}")
                context.showToast("Database error, can't insert new items: ${e.localizedMessage}")
            }
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            mainJob = scope.launch {
                Log.d("AppDatabase", "Creating database")
                insertAll()
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch {
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
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope,
            appManager: AppManager
        ): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, scope, appManager).also { appDatabase ->
                    INSTANCE = appDatabase
                }
            }

        private fun buildDatabase(
            context: Context,
            scope: CoroutineScope,
            appManager: AppManager
        ) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, DATABASE_NAME
            )
                .addCallback(AppDatabaseCallback(context, scope, appManager))
                .build()
    }
}