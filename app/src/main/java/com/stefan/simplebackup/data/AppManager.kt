package com.stefan.simplebackup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

class AppManager(private val context: Context) {

    private val packageSharedPref = context.getSharedPreferences("package", Context.MODE_PRIVATE)
    private val getSavedSequenceNumber get() = packageSharedPref.getInt("sequence_number", 0)
    private val getChangedPackages get() = packageManager.getChangedPackages(getSavedSequenceNumber)

    /**
     * - Sadrži [PackageManager]
     */
    private val packageManager: PackageManager = context.packageManager

    /**
     * - Vrati kreiran [AppData] objekat
     */
    fun build(packageName: String) = getAppObject(getPackageApplicationInfo(packageName))

    fun printSequence() {
        Log.d("AppManager", "Sequence number: ${packageSharedPref.getInt("sequence_number", 0)}")
    }

    private fun saveSequenceNumber(newSequenceNumber: Int) {
        if (newSequenceNumber != getSavedSequenceNumber) {
            packageSharedPref.apply {
                edit()
                    .putInt("sequence_number", newSequenceNumber)
                    .apply()
            }
        }
        Log.d("AppManager", "Sequence number: ${packageSharedPref.getInt("sequence_number", 0)}")
    }

    fun updateSequenceNumber() {
        val changedPackages = getChangedPackages
        changedPackages?.let { changed ->
            saveSequenceNumber(changed.sequenceNumber)
        }
    }

    fun getChangedPackageNames(): Flow<String> = flow {
        val changedPackages = getChangedPackages
        changedPackages?.let { changed ->
            saveSequenceNumber(changed.sequenceNumber)
            changed.packageNames.forEach { packageName ->
                emit(packageName)
            }
        }
    }

    fun doesPackageExists(packageName: String): Boolean {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return true
    }

    private fun getPackageApplicationInfo(packageName: String) =
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

    /**
     * - Koristi se kada se Database prvi put kreira.
     */
    suspend fun getApplicationList(): MutableList<AppData> {
        var time: Long
        val userAppsList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        /**
         * - Prazna application HashMap lista u koju kasnije dodajemo [AppData] objekte
         * - Mora biti val jer ostali thread-ovi upisuju u nju
         */
        val applicationHashMap = ConcurrentHashMap<Int, AppData>()

        withContext(Dispatchers.IO) {
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
        Log.d("AppManager", "Load time: $time")
        return applicationHashMap.values.toMutableList()
    }

    /**
     * - Kreira objekte [AppData] klase
     *
     * - Android 11 po defaultu ne prikazuje sve informacije instaliranih aplikacija.
     *   To se može zaobići u AndroidManifest.xml fajlu dodavanjem
     *   **<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     *   tools:ignore="QueryAllPackagesPermission" />**
     */
    private fun getAppObject(appInfo: ApplicationInfo): Flow<AppData> = flow {
        val packageName = context.applicationContext.packageName
        if (!(isSystemApp(appInfo) || appInfo.packageName.equals(packageName))) {
            val apkDir = appInfo.publicSourceDir.run { substringBeforeLast("/") }
            val name = appInfo.loadLabel(packageManager).toString()
            val drawable = appInfo.loadIcon(packageManager)
            val versionName = packageManager.getPackageInfo(
                appInfo.packageName,
                PackageManager.GET_META_DATA
            ).versionName.run { substringBefore(" (") }

            //Log.d("AppManager", "Apk file list: ${listApkLibs(File(appInfo.publicSourceDir))}")

            val application = AppData(
                0,
                name,
                FileUtil.drawableToByteArray(drawable),
                appInfo.packageName,
                versionName,
                appInfo.targetSdkVersion,
                appInfo.minSdkVersion,
                appInfo.dataDir,
                apkDir,
                getApkSize(apkDir),
                false
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
        return withContext(Dispatchers.IO) {
            val dir = File(path)
            val listFiles = dir.listFiles()
            if (!listFiles.isNullOrEmpty()) {
                listFiles.filter {
                    it.isFile && it.name.endsWith(".apk")
                }.sumOf {
                    it.length()
                }.toFloat()
            } else {
                0f
            }
        }
    }

    private suspend fun listApkLibs(apkFile: File): List<String> {
        return withContext(Dispatchers.IO) {
            val abiList = mutableListOf<String>()
            runCatching {
                ZipInputStream(FileInputStream(apkFile)).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        Log.d("AppManager", "Entry name: ${entry.name}")
                        entry = zipInputStream.nextEntry
                    }
                }
            }.onFailure { throwable ->
                throwable.message?.let { message ->
                    Log.e(
                        "AppManager",
                        "${apkFile.name}: $message"
                    )
                }
            }
            abiList.toList()
        }
    }
}