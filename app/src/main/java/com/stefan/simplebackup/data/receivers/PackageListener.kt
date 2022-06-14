package com.stefan.simplebackup.data.receivers

import com.stefan.simplebackup.data.local.repository.AppRepository

interface PackageListener {
    fun getRepository(): AppRepository

    suspend fun insertOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)

    suspend fun refreshPackageList()
}