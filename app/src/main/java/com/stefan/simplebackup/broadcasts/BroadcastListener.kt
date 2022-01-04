package com.stefan.simplebackup.broadcasts

interface BroadcastListener {
    suspend fun addOrUpdatePackage(packageName: String)

    suspend fun deletePackage(packageName: String)
}