package com.stefan.simplebackup.utils

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootChecker(context: Context) {

    private val rootContext = context

    suspend fun isRooted(): Boolean {
        return hasSuBinary() || hasRootManagerApp(rootContext)
    }

    fun hasRootAccess(): Boolean {
        return Shell.rootAccess()
    }

    private fun hasRootManagerApp(context: Context): Boolean {
        val rootPackageName = "com.topjohnwu.magisk"
            try {
                context.packageManager.getApplicationInfo(rootPackageName, 0)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        return false
    }

    private suspend fun hasSuBinary(): Boolean {
        return withContext(Dispatchers.IO) {
            val paths = System.getenv("PATH")
            if (!paths.isNullOrBlank()) {
                val systemPaths: List<String> = paths.split(":")
                Log.d("path", systemPaths.toString())
                systemPaths.firstOrNull { File(it, "su").exists() } != null
            } else {
                // Postavi standardne su binary putanje ako System PATH environment putanje nisu dostupne
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
                // Vrati prvi koji postoji (ako je true za exist()). Takva vrednost je != null, funkcija vraća true
                // Ako ne postoji (false je za exist()) onda vrati null, pošto null nije != null, funkcija vraća false
                // it predstavlja izabranu putanju, a su binary fajl.
                binaryPath.firstOrNull { File(it, "su").exists() } != null
            }
        }
    }
}