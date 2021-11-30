package com.stefan.simplebackup.database

import androidx.room.*
import com.stefan.simplebackup.data.Application

@Dao
interface AppDao {
    @Query("SELECT * FROM app_table ORDER BY name ASC")
    fun getAppList(): MutableList<Application>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(app: Application)

    @Query("DELETE FROM app_table")
    fun clear()
}