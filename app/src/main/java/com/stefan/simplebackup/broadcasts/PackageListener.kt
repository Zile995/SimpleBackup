package com.stefan.simplebackup.broadcasts

interface PackageListener {
    suspend fun addOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)
}