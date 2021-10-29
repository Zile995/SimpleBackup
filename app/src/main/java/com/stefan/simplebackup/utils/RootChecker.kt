package com.stefan.simplebackup.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class RootChecker(context: Context) {

    private val rootContext = context

    suspend fun isRooted(checkForRootManager: Boolean): Boolean {
        return hasSuBinary() || (checkForRootManager && hasRootManagerApp(
            rootContext
        ))
    }

    suspend fun hasRootAccess(): Boolean {
        return if(hasSuBinary()) {
            val process = Runtime.getRuntime().exec("su -c cd / && ls").inputStream
            BufferedReader(InputStreamReader(process)).use {
                !it.readLine().isNullOrEmpty()
            }
        } else
            false
    }

    private fun hasRootManagerApp(context: Context): Boolean {
        val rootPackageNames = arrayOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "me.phh.superuser",
            "com.koushikdutta.superuser"
        )
        rootPackageNames.forEach {
            try {
                println(context.packageManager.getApplicationInfo(it, 0))
                context.packageManager.getApplicationInfo(it, 0)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private suspend fun hasSuBinary(): Boolean {
        return try {
            findSuBinary()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun findSuBinary(): Boolean {
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