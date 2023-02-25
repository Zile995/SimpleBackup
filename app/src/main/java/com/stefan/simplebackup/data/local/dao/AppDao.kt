package com.stefan.simplebackup.data.local.dao

import androidx.room.*
import com.stefan.simplebackup.data.model.APP_TABLE_NAME
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // Get all AppData from table
    @Transaction
    @Query("SELECT * FROM $APP_TABLE_NAME ORDER BY name ASC")
    fun getAllApps(): Flow<MutableList<AppData>>

    // Search methods
    @Transaction
    @Query("SELECT * FROM $APP_TABLE_NAME WHERE name LIKE :name || '%' AND is_local =:isLocal ORDER BY name ASC")
    fun findAppsByName(name: String, isLocal: Boolean): Flow<MutableList<AppData>>

    // Simple insert and delete methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppData)

    @Query("DELETE FROM $APP_TABLE_NAME WHERE package_name = :packageName AND is_local = :isLocal")
    suspend fun delete(packageName: String, isLocal: Boolean = false): Int

    // Favorite methods
    @Transaction
    suspend fun addToFavorites(packageName: String) = setFavorite(packageName, true)

    @Transaction
    suspend fun removeFromFavorites(packageName: String) = setFavorite(packageName, false)

    @Query("SELECT is_favorite FROM $APP_TABLE_NAME WHERE package_name = :packageName")
    suspend fun isFavorite(packageName: String): Boolean?

    @Query("SELECT version_name FROM $APP_TABLE_NAME WHERE package_name = :packageName AND is_local = 0")
    suspend fun getVersionName(packageName: String): String

    @Query("SELECT apk_dir FROM $APP_TABLE_NAME WHERE package_name = :packageName AND is_local = 0")
    suspend fun getApkDir(packageName: String): String

    @Query("UPDATE $APP_TABLE_NAME SET is_favorite = :setFavorite WHERE package_name = :packageName AND is_local = 0")
    suspend fun setFavorite(packageName: String, setFavorite: Boolean)

    // Get AppData instances
    @Query("SELECT * FROM $APP_TABLE_NAME WHERE package_name = :packageName")
    suspend fun getAppData(packageName: String): AppData

    @Query("SELECT * FROM $APP_TABLE_NAME WHERE package_name = :packageName AND is_local = 1")
    suspend fun getLocalData(packageName: String): AppData

    // Clear complete table
    @Query("DELETE FROM $APP_TABLE_NAME")
    suspend fun clear()
}