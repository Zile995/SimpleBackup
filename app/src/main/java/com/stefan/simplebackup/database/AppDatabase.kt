package com.stefan.simplebackup.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.AppInfo
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
        private val appInfo: AppInfo
    ) :
        RoomDatabase.Callback() {

        private suspend fun insert() {
            val packageList = appInfo.getPackageList()
            if (INSTANCE != null) {
                val appDao = INSTANCE!!.appDao()
                packageList.forEach {
                    appDao.insert(it)
                }
            }
        }

        private suspend fun deleteOrUpdate() {
            if (INSTANCE != null) {
                val appDao = INSTANCE!!.appDao()
                appInfo.getChangedPackageNames().collect { app ->
                    app.forEach { hashMap ->
                        if (hashMap.value) {
                            appInfo.getApp(appInfo.getPackageApplicationInfo(hashMap.key))
                                .collect { app ->
                                    appDao.insert(app)
                                }
                        } else {
                            appDao.delete(hashMap.key)
                        }
                    }
                }
            }
        }


        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                insert()
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

        fun getDbInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "app_database"
                )
                    .addCallback(AppDatabaseCallback(scope, AppInfo(context)))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}