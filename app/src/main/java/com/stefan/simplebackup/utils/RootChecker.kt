package com.stefan.simplebackup.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootChecker(context: Context) {

    private val rootContext = context

    fun isRooted(checkForRootManager: Boolean): Boolean {
        return hasSuBinary() || (checkForRootManager && hasRootManagerApp(
            rootContext
        ))
    }

    fun hasRootAccess(): Boolean {
        val process = Runtime.getRuntime().exec("su -c cd / && ls").inputStream
        BufferedReader(InputStreamReader(process)).use {
            return !it.readLine().isNullOrEmpty()
        }
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
                context.packageManager.getApplicationInfo(it, 0)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun hasSuBinary(): Boolean {
        return try {
            findSuBinary()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun findSuBinary(): Boolean {
        val paths = System.getenv("PATH")
        if (!paths.isNullOrBlank()) {
            val systemPaths: List<String> = paths.split(":")
            Log.d("path", systemPaths.toString())
             systemPaths.firstOrNull { File(it, "su").exists() } != null
        }
        // Postavi standardne su binary putanje ako System PATH environment putanje nisu dostupne
        val binaryPath = arrayOf(
            "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/", "/data/local/bin/",
            "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"
        )
        // Vrati prvi koji postoji (ako je true za exist()). Takva vrednost je != null, funkcija vraća true
        // Ako ne postoji (false je za exist()) onda vrati null, pošto null nije != null, funkcija vraća false
        // it predstavlja izabranu putanju, a su binary fajl.
        return binaryPath.firstOrNull { File(it, "su").exists() } != null
    }
}