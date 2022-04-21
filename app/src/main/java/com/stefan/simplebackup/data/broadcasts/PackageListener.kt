package com.stefan.simplebackup.data.broadcasts

interface PackageListener {
    suspend fun addOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)
}