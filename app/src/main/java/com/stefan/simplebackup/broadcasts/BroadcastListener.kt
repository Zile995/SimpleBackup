package com.stefan.simplebackup.broadcasts

interface BroadcastListener {

    suspend fun addOrUpdatePackages(packageName: String)

    suspend fun removePackages(packageName: String)
}