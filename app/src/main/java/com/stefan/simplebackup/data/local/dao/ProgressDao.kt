package com.stefan.simplebackup.data.local.dao

import androidx.room.*
import com.stefan.simplebackup.data.model.PROGRESS_TABLE_NAME
import com.stefan.simplebackup.data.model.ProgressData
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Transaction
    @Query("SELECT * FROM $PROGRESS_TABLE_NAME")
    fun getProgressData(): Flow<MutableList<ProgressData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ProgressData)

    @Query("DELETE FROM $PROGRESS_TABLE_NAME")
    suspend fun clear()
}