package com.stefan.simplebackup.data.local.dao

import androidx.room.*
import com.stefan.simplebackup.data.model.APP_TABLE_NAME
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Transaction
    @Query("SELECT * FROM $APP_TABLE_NAME ORDER BY name ASC")
    fun getAllApps(): Flow<MutableList<AppData>>

    @Transaction
    @Query("SELECT * FROM $APP_TABLE_NAME WHERE name LIKE :name || '%' ORDER BY name ASC")
    fun findAppsByName(name: String): Flow<MutableList<AppData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: AppData)

    @Query("DELETE FROM $APP_TABLE_NAME WHERE package_name = :packageName")
    suspend fun delete(packageName: String): Int

    @Transaction
    suspend fun addToFavorites(packageName: String) = setFavorite(packageName, true)

    @Transaction
    suspend fun removeFromFavorites(packageName: String) = setFavorite(packageName, false)

    @Query("SELECT is_favorite FROM $APP_TABLE_NAME WHERE package_name = :packageName")
    suspend fun isFavorite(packageName: String): Boolean?

    @Query("UPDATE $APP_TABLE_NAME SET is_favorite = :setFavorite WHERE package_name = :packageName ")
    suspend fun setFavorite(packageName: String, setFavorite: Boolean)

    @Query("SELECT * FROM $APP_TABLE_NAME WHERE package_name = :packageName")
    suspend fun getData(packageName: String): AppData?

    @Query("DELETE FROM $APP_TABLE_NAME")
    suspend fun clear()
}