package com.stefan.simplebackup.data.local.database

import android.database.sqlite.SQLiteException
import androidx.room.*
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.ProgressData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_table ORDER BY name ASC")
    @Transaction
    fun getAllApps(): Flow<MutableList<AppData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Throws(SQLiteException::class)
    suspend fun insert(app: AppData): Long

    @Update
    suspend fun update(app: AppData): Int

    @Transaction
    suspend fun insertOrUpdate(app: AppData) {
        val result = insert(app)
        if (result == -1L)
            update(app)
    }

    @Query("DELETE FROM app_table WHERE package_name = :packageName")
    suspend fun delete(packageName: String): Int

    @Query("DELETE FROM app_table")
    suspend fun clear()

    @Query(
        "DELETE FROM app_table" +
                " WHERE package_name = :packageName AND is_local =1 AND is_local =:selectCloudOnly"
    )
    suspend fun deleteBackup(packageName: String, selectCloudOnly: Boolean = false)

    @Query("SELECT * FROM app_table WHERE uid = :uid")
    suspend fun getData(uid: Int): AppData

    @Query("SELECT * FROM app_table WHERE package_name = :packageName")
    suspend fun getData(packageName: String): AppData

    @Query(
        "SELECT EXISTS(SELECT * FROM app_table" +
                " WHERE package_name =:packageName AND is_local = 1 and is_cloud =:checkCloudOnly)"
    )
    fun doesExist(packageName: String, checkCloudOnly: Boolean = false): Boolean

    @Query(
        "SELECT name, bitmap, package_name, version_name, is_split, is_user_app, favorite" +
                " FROM app_table" +
                " WHERE package_name =:packageName"
    )
    suspend fun getProgressData(packageName: String): ProgressData
}