package com.stefan.simplebackup.data.manager

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.BitmapUtil.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

class AppManager(private val context: Context) {

    /**
     * - [PackageManager]
     */
    private val packageManager: PackageManager = context.packageManager
    private val storageStatsManager by lazy {
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    suspend fun build(packageName: String) =
        withContext(ioDispatcher) {
            getAppData(appInfo = getAppInfo(packageName))
        }

    fun printSequence() {
        Log.d(
            "AppManager", "Sequence number:" +
                    " ${PreferenceHelper.getSequenceNumber}"
        )
    }

    private fun getChangedPackages() =
        packageManager.getChangedPackages(PreferenceHelper.getSequenceNumber)

    private suspend fun saveSequenceNumber(newSequenceNumber: Int) {
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
            changed.packageNames.filter { packageName ->
                packageName != context.packageName
            }.forEach { packageName ->
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

    private fun getAppInfo(packageName: String) =
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

    private fun ApplicationInfo.isUserApp() =
        flags and ApplicationInfo.FLAG_SYSTEM == 0

    // Simple flow which sends data
    fun dataBuilder(includeSystemApps: Boolean = false) = flow {
        val completeAppsInfo = getCompleteAppsInfo()
        completeAppsInfo.apply {
            getFilteredInfo(includeSystemApps).forEach { userAppsInfo ->
                emit(getAppData(userAppsInfo))
            }
        }
    }.flowOn(ioDispatcher)

    private fun getCompleteAppsInfo(): List<ApplicationInfo> {
        return packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                appInfo.packageName != context.packageName
            }.sortedBy { appInfo ->
                appInfo.loadLabel()
            }
    }

    private fun List<ApplicationInfo>.getFilteredInfo(filterSystemApps: Boolean = false) =
        filter { appInfo ->
            if (filterSystemApps) !appInfo.isUserApp() else appInfo.isUserApp()
        }

    private fun ApplicationInfo.loadLabel() = loadLabel(packageManager).toString()

    private suspend fun getAppData(appInfo: ApplicationInfo): AppData = coroutineScope {
        val storageStats: StorageStats =
            storageStatsManager.queryStatsForUid(appInfo.storageUuid, appInfo.uid)
        val cacheSize = storageStats.cacheBytes
        val dataSize = storageStats.dataBytes
        val apkDir = appInfo.publicSourceDir.run { substringBeforeLast("/") }
        val apkInfo = async { getApkInfo(apkDir) }
        val name = appInfo.loadLabel(packageManager).toString()
        val packageName = appInfo.packageName
        val drawable = appInfo.loadIcon(packageManager)
        val versionName = packageManager.getPackageInfo(
            appInfo.packageName,
            PackageManager.GET_META_DATA
        ).versionName?.substringBefore(" (") ?: ""


        return@coroutineScope AppData(
            name = name,
            bitmap = drawable.toByteArray(),
            packageName = packageName,
            versionName = versionName,
            targetSdk = appInfo.targetSdkVersion,
            minSdk = appInfo.minSdkVersion,
            dataDir = appInfo.dataDir,
            apkDir = apkDir,
            apkSize = apkInfo.await()!!.first,
            isSplit = apkInfo.await()!!.second,
            dataSize = dataSize,
            cacheSize = cacheSize,
            isUserApp = appInfo.isUserApp(),
            favorite = false
        )
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