package com.stefan.simplebackup.data.receivers

interface PackageListener {
    suspend fun addOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)
}