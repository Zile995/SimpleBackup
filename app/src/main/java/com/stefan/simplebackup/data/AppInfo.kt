package com.stefan.simplebackup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

object AppInfo {
    private var userAppsList = mutableListOf<ApplicationInfo>()
    private var applicationList = mutableListOf<Application>()
    private lateinit var pm: PackageManager

    fun loadPackageManager(context: Context): AppInfo {
        pm = context.packageManager
        return this
    }

    val getAppList get() = applicationList

    val getPackageManager get() = pm

    fun getAppInfo() = userAppsList

    suspend fun loadAppInfo(flags: Int): AppInfo {
        withContext(Dispatchers.IO) {
            userAppsList = pm.getInstalledApplications(flags)
        }
        return this
    }

    suspend fun getPackageList(context: Context) {
        withContext(Dispatchers.IO) {
            applicationList.clear()
            val size = userAppsList.size

            val quarter = (size / 4)
            val secondQuarter = size / 2
            val thirdQuarter = size - quarter

            val time = measureTimeMillis {
                launch {
                    for (i in 0 until quarter) {
                        insertApp(userAppsList[i], context)
                    }
                }
                launch {
                    for (i in quarter until secondQuarter) {
                        insertApp(userAppsList[i], context)
                    }
                }
                launch {
                    for (i in secondQuarter until thirdQuarter) {
                        insertApp(userAppsList[i], context)
                    }
                }
                launch {
                    for (i in thirdQuarter until size) {
                        insertApp(userAppsList[i], context)
                    }
                }.join()
            }
            println("Thread time: $time")
        }
        applicationList.sortBy { it.getName() }
    }

    /**
     * - Puni MutableList sa izdvojenim objektima Application klase
     *
     * - pm je isntanca PackageManager klase pomoću koje dobavljamo sve informacije o aplikacijama
     *
     * - SuppressLint ignoriše upozorenja vezana za getInstalledApplications,
     *   jer Android 11 po defaultu ne prikazuje sve informacije instaliranih aplikacija.
     *   To se može zaobići u AndroidManifest.xml fajlu dodavanjem
     *   **<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     *   tools:ignore="QueryAllPackagesPermission" />**
     */
    private suspend fun insertApp(appInfo: ApplicationInfo, context: Context) {
        val packageName = context.applicationContext.packageName
        if (!(isSystemApp(appInfo) || appInfo.packageName.equals(packageName))) {
            val apkDir = appInfo.publicSourceDir.removeSuffix("/base.apk")
            val name = appInfo.loadLabel(pm).toString()
            val drawable = appInfo.loadIcon(pm)
            val versionName = pm.getPackageInfo(
                appInfo.packageName,
                PackageManager.GET_META_DATA
            ).versionName
            applicationList.add(
                Application(
                    0,
                    name,
                    FileUtil.drawableToByteArray(drawable),
                    appInfo.packageName,
                    versionName,
                    appInfo.targetSdkVersion,
                    appInfo.minSdkVersion,
                    appInfo.dataDir,
                    apkDir,
                    "",
                    "",
                    getApkSize(apkDir)
                )
            )
        }
    }

    /**
     * - Proverava da li je prosleđena aplikacija system app
     */
    private fun isSystemApp(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    private suspend fun getApkSize(path: String): Float {
        return withContext(Dispatchers.Default) {
            val dir = File(path)
            val listFiles = dir.listFiles()
            if (!listFiles.isNullOrEmpty()) {
                listFiles.filter {
                    it.isFile && it.name.endsWith(".apk")
                }.map {
                    it.length()
                }.sum().toFloat()
            } else {
                0f
            }
        }
    }

}