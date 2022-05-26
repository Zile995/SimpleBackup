package com.stefan.simplebackup.data.receivers

interface PackageListener {
    suspend fun insertOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)

    suspend fun refreshPackageList()
}