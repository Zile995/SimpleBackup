package com.stefan.simplebackup.database

import androidx.room.*
import com.stefan.simplebackup.data.Application
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_table ORDER BY name ASC")
    @Transaction
    fun getAppList(): Flow<MutableList<Application>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: Application)

    @Query("DELETE FROM app_table")
    suspend fun clear()

    @Query("DELETE FROM app_table WHERE package_name = :packageName")
    suspend fun delete(packageName: String)
}