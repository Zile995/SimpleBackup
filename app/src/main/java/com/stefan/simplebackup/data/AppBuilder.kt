package com.stefan.simplebackup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

class AppBuilder(private val context: Context) {

    private var sequenceNumber: Int = 0

    /**
     * - Sadrži [PackageManager]
     */
    private val packageManager: PackageManager = context.packageManager

    /**
     * - Prazna application HashMap lista u koju kasnije dodajemo [Application] objekte
     * - Mora biti val jer ostali thread-ovi upisuju u nju
     */
    private val applicationHashMap = ConcurrentHashMap<Int, Application>()

    /**
     * - Vrati kreiran [Application] objekat
     */
    fun getApp(packageName: String) = getAppObject(getPackageApplicationInfo(packageName))

    fun getChangedPackageNames(): Flow<HashMap<String, Boolean>> = flow {
        val hashMap = HashMap<String, Boolean>()
        val changedPackages = packageManager.getChangedPackages(sequenceNumber)
        changedPackages?.let {
            sequenceNumber = it.sequenceNumber
            for (i in 0 until it.packageNames.size) {
                val packageName = it.packageNames[i]
                hashMap[packageName] = doesPackageExists(packageName)
                emit(hashMap)
            }
        }
    }

    private fun doesPackageExists(packageName: String): Boolean {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return true
    }

    private fun getPackageApplicationInfo(packageName: String) = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

    /**
     * - Vrat
     * - Koristi se kada se Database prvi put kreira.
     */
    suspend fun getApplicationList(): MutableList<Application> {
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
                        getAppObject(userAppsList[i]).collect { app -> applicationHashMap[i] = app }
                    }
                }
                launch {
                    for (i in quarter until secondQuarter) {
                        getAppObject(userAppsList[i]).collect { app -> applicationHashMap[i] = app }
                    }
                }
                launch {
                    for (i in secondQuarter until thirdQuarter) {
                        getAppObject(userAppsList[i]).collect { app -> applicationHashMap[i] = app }
                    }
                }
                launch {
                    for (i in thirdQuarter until size) {
                        getAppObject(userAppsList[i]).collect { app -> applicationHashMap[i] = app }
                    }
                }.join()
            }
        }
        println("Thread time: $time")
        return applicationHashMap.values.toMutableList()
    }

    /**
     * - Kreira objekte [Application] klase
     *
     * - Android 11 po defaultu ne prikazuje sve informacije instaliranih aplikacija.
     *   To se može zaobići u AndroidManifest.xml fajlu dodavanjem
     *   **<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     *   tools:ignore="QueryAllPackagesPermission" />**
     */
    private fun getAppObject(appInfo: ApplicationInfo): Flow<Application> = flow {
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

            emit(application)
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