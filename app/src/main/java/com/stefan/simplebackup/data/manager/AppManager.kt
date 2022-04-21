package com.stefan.simplebackup.data.manager

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.BitmapUtil
import com.stefan.simplebackup.utils.main.PreferenceHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class AppManager(private val context: Context) {

    /**
     * - IO Dispatcher
     */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * - Sadrži [PackageManager]
     */
    private val packageManager: PackageManager = context.packageManager
    private val storageStatsManager by lazy {
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    /**
     * - Vrati kreiran [AppData] objekat
     */
    fun build(packageName: String) = getAppData(getAppInfoByPackageName(packageName))

    fun printSequence() {
        Log.d(
            "AppManager", "Sequence number:" +
                    " ${PreferenceHelper.getSequenceNumber}"
        )
    }

    private fun getChangedPackages() =
        packageManager.getChangedPackages(PreferenceHelper.getSequenceNumber)

    private fun saveSequenceNumber(newSequenceNumber: Int) {
        if (newSequenceNumber != PreferenceHelper.getSequenceNumber) {
            PreferenceHelper.updateSequenceNumber(newSequenceNumber)
        }
        Log.d(
            "AppManager",
            "Sequence number: ${PreferenceHelper.getSequenceNumber}"
        )
    }

    suspend fun updateSequenceNumber() {
        withContext(ioDispatcher) {
            val changedPackages = getChangedPackages()
            changedPackages?.let { changed ->
                saveSequenceNumber(changed.sequenceNumber)
            }
        }
    }

    fun getChangedPackageNames(): Flow<String> = flow {
        val changedPackages = getChangedPackages()
        changedPackages?.let { changed ->
            saveSequenceNumber(changed.sequenceNumber)
            changed.packageNames.forEach { packageName ->
                emit(packageName)
            }
        }
    }.flowOn(ioDispatcher)

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
            val userAppsInfo = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val size = userAppsInfo.size
            val quarter = (size / 4)
            val secondQuarter = size / 2
            val thirdQuarter = size - quarter

            launch {
                for (i in 0 until quarter) {
                    getAppData(userAppsInfo[i]).collect { app -> send(app) }
                }
            }
            launch {
                for (i in quarter until secondQuarter) {
                    getAppData(userAppsInfo[i]).collect { app -> send(app) }
                }
            }
            launch {
                for (i in secondQuarter until thirdQuarter) {
                    getAppData(userAppsInfo[i]).collect { app -> send(app) }
                }
            }
            launch {
                for (i in thirdQuarter until size) {
                    getAppData(userAppsInfo[i]).collect { app -> send(app) }
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
    private fun getAppData(appInfo: ApplicationInfo): Flow<AppData> = flow {
        val myPackageName = context.applicationContext.packageName
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
            val apkSize = apkInfo?.first
            val isSplit = apkInfo?.second

            val application = AppData(
                uid = 0,
                name = name,
                bitmap = BitmapUtil.drawableToByteArray(drawable),
                packageName = packageName,
                versionName = versionName,
                targetSdk = appInfo.targetSdkVersion,
                minSdk = appInfo.minSdkVersion,
                dataDir = appInfo.dataDir,
                apkDir = apkDir,
                apkSize = apkSize ?: 0f,
                split = isSplit ?: false,
                dataSize = dataSize,
                cacheSize = cacheSize,
                favorite = false
            )
            emit(application)
        }
    }.flowOn(ioDispatcher)

    /**
     * - Proverava da li je prosleđena aplikacija system app
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    private suspend fun getApkInfo(apkDirPath: String): Pair<Float, Boolean>? {
        return withContext(ioDispatcher) {
            val isSplit: Boolean
            File(apkDirPath).listFiles()?.let { apkDirFiles ->
                apkDirFiles.filter { dirFile ->
                    dirFile.isFile && dirFile.name.endsWith(".apk")
                }.also { apkFiles ->
                    isSplit = apkFiles.size > 1
                }.sumOf { apkFile ->
                    apkFile.length()
                }.toFloat() to isSplit
            }
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