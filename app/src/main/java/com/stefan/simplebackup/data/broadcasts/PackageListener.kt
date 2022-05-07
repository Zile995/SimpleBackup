package com.stefan.simplebackup.data.broadcasts

import kotlinx.coroutines.CoroutineScope

interface PackageListener {
    fun CoroutineScope.addOrUpdatePackage(packageName: String)

    fun CoroutineScope.deletePackage(packageName: String)
}