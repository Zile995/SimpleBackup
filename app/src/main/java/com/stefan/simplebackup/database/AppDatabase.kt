package com.stefan.simplebackup.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.data.AppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

        private val appDao by lazy { INSTANCE?.appDao() }

        private suspend fun insertAll() {
            val appList = appManager.getApplicationList()
            appManager.updateSequenceNumber()
            appList.forEach { app ->
                appDao?.insert(app)
            }
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                insertAll()
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