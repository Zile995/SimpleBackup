package com.stefan.simplebackup.data.manager

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.graphics.drawable.Drawable
import android.os.Build

class AppInfoManager(private val packageManager: PackageManager, private val flag: Long) {
    @Suppress("DEPRECATION")
    fun getCompleteAppsInfo(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            packageManager.getInstalledApplications(ApplicationInfoFlags.of(flag))
        else
            packageManager.getInstalledApplications(flag.toInt())

    @Suppress("DEPRECATION")
    @Throws(NameNotFoundException::class)
    fun getAppInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            packageManager.getApplicationInfo(packageName, ApplicationInfoFlags.of(flag))
        else
            packageManager.getApplicationInfo(packageName, flag.toInt())

    @Suppress("DEPRECATION")
    @Throws(NameNotFoundException::class)
    fun getPackageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            packageManager.getPackageInfo(packageName, PackageInfoFlags.of(flag))
        else
            packageManager.getPackageInfo(packageName, flag.toInt())

    inline fun filterAppsInfo(
        filterSystemApps: Boolean = false,
        predicate: (ApplicationInfo) -> Boolean
    ) = getCompleteAppsInfo().filter { appInfo ->
        predicate(appInfo) && if (filterSystemApps) !isUserApp(appInfo) else isUserApp(appInfo)
    }

    fun getDataDir(applicationInfo: ApplicationInfo): String = applicationInfo.dataDir

    fun getMinSdk(applicationInfo: ApplicationInfo) = applicationInfo.minSdkVersion
    fun getTargetSdk(applicationInfo: ApplicationInfo) = applicationInfo.targetSdkVersion

    fun getVersionName(applicationInfo: ApplicationInfo) =
        getPackageInfo(applicationInfo.packageName).versionName?.substringBefore(" (") ?: ""

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

    fun getFirstInstallTime(packageName: String) = getPackageInfo(packageName).firstInstallTime
}