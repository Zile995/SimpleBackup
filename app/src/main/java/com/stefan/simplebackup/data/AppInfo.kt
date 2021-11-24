package com.stefan.simplebackup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppInfo {
    private var applicationInfoList = mutableListOf<ApplicationInfo>()
    private lateinit var pm: PackageManager

    fun loadPackageManager(context: Context): AppInfo {
        pm = context.packageManager
        return this
    }

    fun getAppInfo() = applicationInfoList

    fun getPackageManager() = pm

    suspend fun loadAppInfo(flags: Int) {
        withContext(Dispatchers.IO) {
                applicationInfoList = pm.getInstalledApplications(flags)
        }
    }
}