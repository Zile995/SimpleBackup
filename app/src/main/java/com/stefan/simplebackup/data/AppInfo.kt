package com.stefan.simplebackup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.stefan.simplebackup.database.AppDatabase
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

object AppInfo {
    // Zapamti naziv baze podataka
    private const val DATABASE_NAME: String = "app_database"

    // Prazne liste u koje kasnije dodajemo odgovarajuće elemente
    private var userAppsList = mutableListOf<ApplicationInfo>()
    private var applicationHashMap = ConcurrentHashMap<Int, Application>()
    private var applicationList = mutableListOf<Application>()

    // Late init varijable, inicijalizujemo ih u loadPackageManager funkciji
    private lateinit var pm: PackageManager
    private lateinit var appDatabase: AppDatabase

    /**
     * - Učitava [PackageManager] i [AppDatabase]
     */
    fun loadPackageManager(context: Context): AppInfo {
        pm = context.packageManager
        appDatabase = AppDatabase.getDbInstance(context.applicationContext)
        return this
    }

    /**
     * - Vraća [applicationList]
     */
    val getUserAppList get() = applicationList

    /**
     * - Vraća [pm]
     */
    val getPackageManager get() = pm

    fun getAppInfo() = userAppsList

    val getDatabase get() = appDatabase

    /**
     * - Puni [userAppsList]
     * - Vraća referencu [AppInfo] objekta
     */
    suspend fun getInstalledApplications(flags: Int): AppInfo {
        withContext(Dispatchers.Default) {
            userAppsList = pm.getInstalledApplications(flags)
        }
        return this
    }

    fun databaseExists(context: Context) =
        context.getDatabasePath(DATABASE_NAME).exists()

    suspend fun getDatabaseList(): MutableList<Application> {
        return withContext(Dispatchers.Default) {
            applicationList = appDatabase.appDao().getAppList()
            applicationList
        }
    }

    /**
     * Postavi listu
     */
    suspend fun setPackageList(context: Context): MutableList<Application> {
        var time: Long
        withContext(Dispatchers.IO) {
            applicationHashMap.clear()

            val size = userAppsList.size
            val quarter = (size / 4)
            val secondQuarter = size / 2
            val thirdQuarter = size - quarter

            time = measureTimeMillis {
                launch {
                    for (i in 0 until quarter) {
                        insertApp(context, userAppsList[i], i)
                    }
                }
                launch {
                    for (i in quarter until secondQuarter) {
                        insertApp(context, userAppsList[i], i)
                    }
                }
                launch {
                    for (i in secondQuarter until thirdQuarter) {
                        insertApp(context, userAppsList[i], i)
                    }
                }
                launch {
                    for (i in thirdQuarter until size) {
                        insertApp(context, userAppsList[i], i)
                    }
                }.join()
            }
        }
        println("Thread time: $time")
        applicationList = applicationHashMap.values.toMutableList()
        applicationList.sortBy { it.getName() }
        applicationHashMap.clear()
        return applicationList
    }

    suspend fun makeDatabase() {
        withContext(Dispatchers.IO) {
            appDatabase.appDao().clear()
            applicationList.forEach {
                appDatabase.appDao().insert(it)
            }
        }
    }

    /**
     * - Puni MutableList sa izdvojenim objektima [Application] klase
     *
     * - Android 11 po defaultu ne prikazuje sve informacije instaliranih aplikacija.
     *   To se može zaobići u AndroidManifest.xml fajlu dodavanjem
     *   **<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     *   tools:ignore="QueryAllPackagesPermission" />**
     */
    private suspend fun insertApp(context: Context, appInfo: ApplicationInfo, key: Int) {
        withContext(Dispatchers.IO) {
            val packageName = context.applicationContext.packageName
            if (!(isSystemApp(appInfo) || appInfo.packageName.equals(packageName))) {
                val apkDir = appInfo.publicSourceDir.run { substringBeforeLast("/") }
                val name = appInfo.loadLabel(pm).toString()
                val drawable = appInfo.loadIcon(pm)
                val versionName = pm.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_META_DATA
                ).versionName

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