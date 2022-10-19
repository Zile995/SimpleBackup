package com.stefan.simplebackup.utils.root

import android.content.Context
import android.content.pm.PackageManager
import com.stefan.simplebackup.data.manager.AppInfoManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootChecker(private val rootContext: Context) {

    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    suspend fun isDeviceRooted(): Boolean {
        return hasSuBinary() || hasRootManagerApp(rootContext)
    }

    fun hasRootAccess(): Boolean? = Shell.isAppGrantedRoot()

    private fun hasRootManagerApp(context: Context): Boolean {
        val rootPackageName = "com.topjohnwu.magisk"
        return try {
            val appInfoManager = AppInfoManager(packageManager = context.packageManager, 0L)
            appInfoManager.getAppInfo(rootPackageName)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private suspend fun hasSuBinary(): Boolean {
        return withContext(defaultDispatcher) {
            val paths = System.getenv("PATH")
            if (!paths.isNullOrBlank()) {
                val systemPaths: List<String> = paths.split(":")
                systemPaths.firstOrNull { File(it, "su").exists() } != null
            } else {
                val binaryPath = arrayOf(
                    "/sbin/",
                    "/system/bin/",
                    "/system/xbin/",
                    "/data/local/xbin/",
                    "/data/local/bin/",
                    "/system/sd/xbin/",
                    "/system/bin/failsafe/",
                    "/data/local/"
                )
                binaryPath.firstOrNull { File(it, "su").exists() } != null
            }
        }
    }
}