package com.stefan.simplebackup.data

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class AppManager(private val context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val packageSharedPref = context.getSharedPreferences("package", Context.MODE_PRIVATE)
    private val getSavedSequenceNumber get() = packageSharedPref.getInt("sequence_number", 0)
    private val getChangedPackages get() = packageManager.getChangedPackages(getSavedSequenceNumber)

    /**
     * - Sadrži [PackageManager]
     */
    private val packageManager: PackageManager = context.packageManager

    private val myPackageName by lazy { context.applicationContext.packageName }
    private val storageStatsManager by lazy {
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    private val getAllUserAppsInfo: List<ApplicationInfo>
        get() {
            return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }

    /**
     * - Vrati kreiran [AppData] objekat
     */
    fun build(packageName: String) = getAppObject(getAppInfoByPackageName(packageName))

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

    private fun getAppInfoByPackageName(packageName: String) =
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

    /**
     * - Koristi se kada se Database prvi put kreira.
     */
    suspend fun getApplicationList(): Flow<AppData> = channelFlow {
        withContext(ioDispatcher) {
            val userAppsInfo = getAllUserAppsInfo

            val size = userAppsInfo.size
            val quarter = (size / 4)
            val secondQuarter = size / 2
            val thirdQuarter = size - quarter

            launch {
                for (i in 0 until quarter) {
                    getAppObject(userAppsInfo[i]).collect { app -> send(app) }
                }
            }
            launch {
                for (i in quarter until secondQuarter) {
                    getAppObject(userAppsInfo[i]).collect { app -> send(app) }
                }
            }
            launch {
                for (i in secondQuarter until thirdQuarter) {
                    getAppObject(userAppsInfo[i]).collect { app -> send(app) }
                }
            }
            launch {
                for (i in thirdQuarter until size) {
                    getAppObject(userAppsInfo[i]).collect { app -> send(app) }
                }
            }.join()
        }
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
        if (!(isSystemApp(appInfo) || appInfo.packageName.equals(myPackageName))) {
            val storageStats: StorageStats =
                storageStatsManager.queryStatsForUid(appInfo.storageUuid, appInfo.uid)
            val cacheSize = storageStats.cacheBytes
            val dataSize = storageStats.dataBytes
            val apkDir = appInfo.publicSourceDir.run { substringBeforeLast("/") }
            val name = appInfo.loadLabel(packageManager).toString()
            val packageName = appInfo.packageName
            val drawable = appInfo.loadIcon(packageManager)
            val versionName = packageManager.getPackageInfo(
                appInfo.packageName,
                PackageManager.GET_META_DATA
            ).versionName?.substringBefore(" (") ?: ""
            val apkInfo = getApkInfo(apkDir)
            val apkSize = apkInfo.first
            val isSplit = apkInfo.second

            val application = AppData(
                0,
                name,
                FileUtil.drawableToByteArray(drawable),
                packageName,
                versionName,
                appInfo.targetSdkVersion,
                appInfo.minSdkVersion,
                appInfo.dataDir,
                apkDir,
                apkSize,
                isSplit,
                dataSize,
                cacheSize,
                false
            )
            emit(application)
        }
    }

    /**
     * - Proverava da li je prosleđena aplikacija system app
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    private suspend fun getApkInfo(apkDirPath: String): Pair<Float, Boolean> {
        return withContext(ioDispatcher) {
            var isSplit = false
            var apkSize = 0f
            val dir = File(apkDirPath)
            val listFiles = dir.listFiles()
            if (!listFiles.isNullOrEmpty()) {
                apkSize = listFiles.filter {
                    it.isFile && it.name.endsWith(".apk")
                }.apply {
                    if (this.size > 1) isSplit = true
                }.sumOf {
                    it.length()
                }.toFloat()
            }
            Pair(apkSize, isSplit)
        }
    }

    private suspend fun listApkLibs(apkFile: File): List<String> {
        return withContext(ioDispatcher) {
            val abiList = mutableListOf<String>()
            runCatching {
                val zipFile = ZipFile(apkFile)
                val headerList = zipFile.fileHeaders
                abiList.addAll(headerList.map { fileHeader ->
                    fileHeader.fileName
                }.filter { fileName ->
                    fileName.contains("lib") && fileName.endsWith(".so")
                }.map {
                    it.substringAfter("/").substringBeforeLast("/")
                }.distinct())
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