package com.stefan.simplebackup.data.local.database

import android.database.sqlite.SQLiteException
import androidx.room.*
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Transaction
    @Query("SELECT * FROM app_table ORDER BY name ASC")
    fun getAllApps(): Flow<MutableList<AppData>>

    @Transaction
    @Query("SELECT * FROM app_table WHERE name LIKE :name || '%' ORDER BY name ASC")
    fun findAppsByName(name: String): Flow<MutableList<AppData>>

    @Throws(SQLiteException::class)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppData)

    @Update
    suspend fun update(app: AppData): Int

    @Query("DELETE FROM app_table WHERE package_name = :packageName")
    suspend fun delete(packageName: String): Int

    @Transaction
    suspend fun removeFromFavorites(packageName: String) = setFavorite(packageName, false)

    @Transaction
    suspend fun addToFavorites(packageName: String) = setFavorite(packageName, true)

    @Query("SELECT favorite FROM app_table WHERE package_name = :packageName")
    suspend fun isFavorite(packageName: String): Boolean?

    @Query("UPDATE app_table SET favorite = :setFavorite WHERE package_name = :packageName ")
    suspend fun setFavorite(packageName: String, setFavorite: Boolean)

    @Query(
        "DELETE FROM app_table" +
                " WHERE package_name = :packageName AND is_local =1 AND is_local =:selectCloudOnly"
    )
    suspend fun deleteBackup(packageName: String, selectCloudOnly: Boolean = false)

    @Query("SELECT * FROM app_table WHERE package_name = :packageName")
    suspend fun getData(packageName: String): AppData?

    @Query("DELETE FROM app_table")
    suspend fun clear()
}