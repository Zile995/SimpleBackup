package com.stefan.simplebackup.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.PreferenceHelper
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
        private val scope: CoroutineScope,
        private val appManager: AppManager
    ) :
        RoomDatabase.Callback() {

        private var mainJob: Job? = null
        private val appDao by lazy { INSTANCE?.appDao() }

        private suspend fun insertAll() {
            val time = measureTimeMillis {
                appManager.apply {
                    updateSequenceNumber()
                    getApplicationList().collect { app ->
                        appDao?.insert(app)
                    }
                }
                PreferenceHelper.setDatabaseCreated(true)
            }
            Log.d("AppDatabase", "Load time: $time")
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

        fun getDbInstance(
            context: Context,
            scope: CoroutineScope,
            appManager: AppManager
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, DATABASE_NAME
                )
                    .addCallback(AppDatabaseCallback(scope, appManager))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}