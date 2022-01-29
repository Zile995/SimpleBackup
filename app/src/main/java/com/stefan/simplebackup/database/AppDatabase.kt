package com.stefan.simplebackup.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Singleton AppData Database klasa
 */
@Database(entities = [AppData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    private class AppDatabaseCallback(
        private val scope: CoroutineScope,
        private val appManager: AppManager
    ) :
        RoomDatabase.Callback() {

        private val appDao by lazy { INSTANCE?.appDao() }

        private suspend fun insertAll() {
            val appList = appManager.getApplicationList()
            appList.forEach { app ->
                appDao?.insert(app)
            }
        }

        private suspend fun deleteOrUpdate() {
            Log.d("AppDatabase", "Calling the deleteOrUpdate()")
            appManager.getChangedPackageNames().collect { packageName ->
                if (appManager.doesPackageExists(packageName)) {
                    appManager.build(packageName).collect { app ->
                        Log.d("AppDatabase", "Adding the $packageName")
                        appDao?.insert(app)
                    }
                } else {
                    Log.d("AppDatabase", "Deleting the $packageName")
                    appDao?.delete(packageName)
                }
            }
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                appManager.updateSequenceNumber()
                insertAll()
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch {
                deleteOrUpdate()
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
                    AppDatabase::class.java, "app_database"
                )
                    .addCallback(AppDatabaseCallback(scope, appManager))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}