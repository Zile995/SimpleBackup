package com.stefan.simplebackup.data.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.toByteArray
import com.stefan.simplebackup.utils.file.FileUtil.getApkFileSizeSplitInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppManager(private val context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * - [PackageManager]
     */
    private val packageManager: PackageManager = context.packageManager

    // Helper manager classes
    private val appInfoManager: AppInfoManager by lazy {
        AppInfoManager(packageManager, 0)
    }

    suspend fun build(packageName: String) = withContext(ioDispatcher) {
        getAppData(appInfo = appInfoManager.getAppInfo(packageName))
    }

    private fun getChangedPackages() =
        packageManager.getChangedPackages(PreferenceHelper.savedSequenceNumber)

    private suspend fun saveSequenceNumber(newSequenceNumber: Int) {
        if (newSequenceNumber != PreferenceHelper.savedSequenceNumber)
            PreferenceHelper.updateSequenceNumber(newSequenceNumber)
        Log.d("AppManager", "Sequence number: ${PreferenceHelper.savedSequenceNumber}")
    }

    suspend fun updateSequenceNumber() {
        withContext(ioDispatcher) {
            val changedPackages = getChangedPackages()
            changedPackages?.let { changed ->
                saveSequenceNumber(changed.sequenceNumber)
            }
        }
    }

    fun getChangedPackageNames() = flow {
        val changedPackages = getChangedPackages()
        changedPackages?.let { changed ->
            changed.packageNames.filter { packageName ->
                packageName != context.packageName
            }.forEach { packageName ->
                emit(packageName)
            }
            saveSequenceNumber(changed.sequenceNumber)
        }
    }

    fun doesPackageExists(packageName: String) = try {
        appInfoManager.getPackageInfo(packageName)
        true
    } catch (e: NameNotFoundException) {
        false
    }

    fun getPackageUid(packageName: String) = try {
        appInfoManager.getAppInfo(packageName).uid
    } catch (e: NameNotFoundException) {
        null
    }

    // Simple flow which sends data
    fun dataBuilder(
        includeSystemApps: Boolean = false, filter: (ApplicationInfo) -> Boolean = { true }
    ) = flow {
        appInfoManager.getFilteredInfo(filterSystemApps = includeSystemApps) { appInfo ->
            appInfo.packageName != context.packageName && filter(appInfo)
        }.forEach { filteredAppsInfo ->
            val app = getAppData(filteredAppsInfo)
            emit(app)
        }
    }.flowOn(ioDispatcher)

    private suspend fun getAppData(appInfo: ApplicationInfo) = coroutineScope {
        appInfoManager.run {
            val apkDir = getApkDir(appInfo)
            val packageName = getPackageName(appInfo)
            val apkInfo = getApkFileSizeSplitInfo(apkDir)

            AppData(
                name = getAppName(appInfo),
                bitmap = getDrawable(appInfo).toByteArray(),
                packageName = packageName,
                versionName = getVersionName(appInfo),
                date = getFirstInstallTime(packageName),
                targetSdk = getTargetSdk(appInfo),
                minSdk = getMinSdk(appInfo),
                dataDir = getDataDir(appInfo),
                apkDir = getApkDir(appInfo),
                apkSize = apkInfo.first,
                isSplit = apkInfo.second,
                isUserApp = isUserApp(appInfo)
            )
        }
    }
}
