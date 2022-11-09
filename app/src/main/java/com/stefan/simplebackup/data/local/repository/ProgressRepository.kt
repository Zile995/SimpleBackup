package com.stefan.simplebackup.data.local.repository

import com.stefan.simplebackup.data.local.dao.ProgressDao
import com.stefan.simplebackup.data.model.ProgressData

class ProgressRepository(private val progressDao: ProgressDao) {

    val progressData get() = progressDao.getProgressData()

    suspend fun insert(progressData: ProgressData) = progressDao.insert(progressData)
    suspend fun clear() = progressDao.clear()
}