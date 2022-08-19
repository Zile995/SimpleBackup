package com.stefan.simplebackup.data.receivers

import com.stefan.simplebackup.data.local.repository.AppRepository

interface PackageListener {
    val repository: AppRepository

    suspend fun refreshPackageList()

    suspend fun insertOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)

    suspend fun onActionPackageAdded(packageName: String)

    suspend fun onActionPackageRemoved(packageName: String)
}