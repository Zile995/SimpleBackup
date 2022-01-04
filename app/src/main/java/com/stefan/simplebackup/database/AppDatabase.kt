package com.stefan.simplebackup.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.AppBuilder
import com.stefan.simplebackup.data.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Singleton Application Database klasa
 */
@Database(entities = [Application::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    private class AppDatabaseCallback(
        private val scope: CoroutineScope,
        private val appBuilder: AppBuilder
    ) :
        RoomDatabase.Callback() {

        private val appDao by lazy { INSTANCE?.appDao() }

        private suspend fun insertAll() {
            val appList = appBuilder.getApplicationList()
            appList.forEach { app ->
                appDao?.insert(app)
            }
        }

        private suspend fun deleteOrUpdate() {
            Log.d("AppDatabase","Calling the deleteOrUpdate()")
            appBuilder.getChangedPackageNames().collect { packageName ->
                if (appBuilder.doesPackageExists(packageName)) {
                    appBuilder.build(packageName).collect { app ->
                        appDao?.insert(app)
                    }
                } else {
                    appDao?.delete(packageName)
                }
            }
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
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
            appBuilder: AppBuilder
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "app_database"
                )
                    .addCallback(AppDatabaseCallback(scope, appBuilder))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}