package com.stefan.simplebackup.data.local.database

import android.database.sqlite.SQLiteException
import androidx.room.*
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_table ORDER BY name ASC")
    @Transaction
    fun getAllApps(): Flow<MutableList<AppData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Throws(SQLiteException::class)
    suspend fun insert(app: AppData)

    @Update
    suspend fun update(app: AppData): Int

    @Query("DELETE FROM app_table WHERE package_name = :packageName")
    suspend fun delete(packageName: String): Int

    @Query("DELETE FROM app_table")
    suspend fun clear()

    @Transaction
    suspend fun updateFavorite(packageName: String) {
        setFavorite(packageName, !(getData(packageName)?.favorite ?: false))
    }

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

    @Query("SELECT EXISTS(SELECT * FROM app_table WHERE package_name = :packageName)")
    fun doesAppDataExist(packageName: String): Boolean
}