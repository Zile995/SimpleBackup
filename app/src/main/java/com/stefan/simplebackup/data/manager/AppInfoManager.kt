package com.stefan.simplebackup.data.manager

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class AppInfoManager(private val packageManager: PackageManager, private val flag: Int) {

    @Suppress("MemberVisibilityCanBePrivate")
    fun getCompleteAppsInfo(): List<ApplicationInfo> =
        packageManager.getInstalledApplications(flag).sortedBy { appInfo ->
            getAppName(appInfo)
        }

    fun getFilteredInfo(
        filterSystemApps: Boolean = false,
        predicate: (ApplicationInfo) -> Boolean
    ) =
        getCompleteAppsInfo().filter { appInfo ->
            predicate(appInfo) && (if (filterSystemApps) !isUserApp(appInfo) else isUserApp(appInfo))
        }

    fun getMinSDK(applicationInfo: ApplicationInfo) = applicationInfo.minSdkVersion
    fun getTargetSDK(applicationInfo: ApplicationInfo) = applicationInfo.targetSdkVersion

    fun getDataDir(applicationInfo: ApplicationInfo): String = applicationInfo.dataDir

    fun getAppInfo(packageName: String, flag: Int) =
        packageManager.getApplicationInfo(packageName, flag)

    fun getVersionName(applicationInfo: ApplicationInfo) =
        packageManager.getPackageInfo(applicationInfo.packageName, flag)
            .versionName?.substringBefore(" (") ?: ""

    fun getDrawable(applicationInfo: ApplicationInfo): Drawable =
        applicationInfo.loadIcon(packageManager)

    fun getPackageName(applicationInfo: ApplicationInfo): String =
        applicationInfo.packageName

    fun getApkDir(applicationInfo: ApplicationInfo) =
        applicationInfo.publicSourceDir.run { substringBeforeLast("/") }

    fun isUserApp(applicationInfo: ApplicationInfo) =
        applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0

    fun getAppName(applicationInfo: ApplicationInfo) =
        applicationInfo.loadLabel(packageManager).toString()

    fun getFirstInstallTime(packageName: String) =
        packageManager.getPackageInfo(packageName, 0).firstInstallTime
}