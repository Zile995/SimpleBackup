package com.stefan.simplebackup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

class AppInfo(private val context: Context) {

     /**
     * - Sadrži [PackageManager]
     */
    private var packageManager: PackageManager = context.packageManager

    // Prazne liste u koje kasnije dodajemo odgovarajuće elemente
    private var applicationHashMap = ConcurrentHashMap<Int, Application>()

    /**
     * Postavi listu
     */
    suspend fun setPackageList(): MutableList<Application> {
        var time: Long
        val userAppsList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        withContext(Dispatchers.IO) {
            applicationHashMap.clear()

            val size = userAppsList.size
            val quarter = (size / 4)
            val secondQuarter = size / 2
            val thirdQuarter = size - quarter

            time = measureTimeMillis {
                launch {
                    for (i in 0 until quarter) {
                        insertApp(userAppsList[i], i)
                    }
                }
                launch {
                    for (i in quarter until secondQuarter) {
                        insertApp(userAppsList[i], i)
                    }
                }
                launch {
                    for (i in secondQuarter until thirdQuarter) {
                        insertApp(userAppsList[i], i)
                    }
                }
                launch {
                    for (i in thirdQuarter until size) {
                        insertApp(userAppsList[i], i)
                    }
                }.join()
            }
        }
        println("Thread time: $time")
        return applicationHashMap.values.toMutableList()
    }


    /**
     * - Puni MutableList sa izdvojenim objektima [Application] klase
     *
     * - Android 11 po defaultu ne prikazuje sve informacije instaliranih aplikacija.
     *   To se može zaobići u AndroidManifest.xml fajlu dodavanjem
     *   **<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     *   tools:ignore="QueryAllPackagesPermission" />**
     */
    private suspend fun insertApp(appInfo: ApplicationInfo, key: Int) {
        withContext(Dispatchers.IO) {
            val packageName = context.applicationContext.packageName
            if (!(isSystemApp(appInfo) || appInfo.packageName.equals(packageName))) {
                val apkDir = appInfo.publicSourceDir.run { substringBeforeLast("/") }
                val name = appInfo.loadLabel(packageManager).toString()
                val drawable = appInfo.loadIcon(packageManager)
                val versionName = packageManager.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_META_DATA
                ).versionName ?: ""

                val application = Application(
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
                    getApkSize(apkDir),
                    1
                )

                applicationHashMap[key] = application
            }
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