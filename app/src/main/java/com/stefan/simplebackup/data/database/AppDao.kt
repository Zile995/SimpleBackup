package com.stefan.simplebackup.data.database

import androidx.room.*
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_table ORDER BY name ASC")
    @Transaction
    fun getAppList(): Flow<MutableList<AppData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppData)

    @Query("DELETE FROM app_table")
    suspend fun clear()

    @Query("DELETE FROM app_table WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM app_table WHERE package_name = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppData
}