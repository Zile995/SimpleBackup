package com.stefan.simplebackup.data.database

import android.database.sqlite.SQLiteException
import androidx.room.*
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.ProgressData
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_table" +
            " WHERE is_user_app =:showUserApps AND is_local = 0 AND is_cloud = 0" +
            " ORDER BY name ASC")
    @Transaction
    fun getAllApps(showUserApps: Boolean = true): Flow<MutableList<AppData>>

    @Query(
        "SELECT * FROM app_table" +
                " WHERE is_user_app = 1 AND is_local = 1 AND is_cloud =:showCloudBackup" +
                " ORDER BY name ASC"
    )
    @Transaction
    fun getLocalList(showCloudBackup: Int = 0): Flow<MutableList<AppData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Throws(SQLiteException::class)
    suspend fun insert(app: AppData)

    @Query("DELETE FROM app_table")
    suspend fun clear()

    @Query("DELETE FROM app_table WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM app_table WHERE uid = :uid")
    suspend fun getData(uid: Int): AppData

    @Query("SELECT * FROM app_table WHERE package_name = :packageName")
    suspend fun getData(packageName: String): AppData

    @Query("SELECT EXISTS(SELECT * FROM app_table WHERE package_name =:packageName AND is_local = 1)")
    fun doesExist(packageName: String): Boolean

    @Query(
        "SELECT name, bitmap, package_name, version_name, is_split, is_user_app, favorite" +
                " FROM app_table WHERE package_name =:packageName"
    )
    suspend fun getProgressAppData(packageName: String): ProgressData
}