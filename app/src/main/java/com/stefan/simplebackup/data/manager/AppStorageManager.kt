package com.stefan.simplebackup.data.manager

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.storage.StorageManager
import android.util.Log
import com.stefan.simplebackup.utils.extensions.convertBytesToMegaBytes
import java.io.IOException

class AppStorageManager(context: Context) {

    private val storageStatsManager by lazy {
        context.applicationContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getFreeStorageSize() = try {
        storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT).convertBytesToMegaBytes()
    } catch (exception: IOException) {
        Log.e("AppStorageManager", "Unable to read storage free space")
        0.0
    }

    fun getTotalStorageSize() = try {
        storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT).convertBytesToMegaBytes()
    } catch (exception: IOException) {
        Log.e("AppStorageManager", "Unable to read storage total space")
        0.0
    }

    fun getUsedStorage() = try {
        getTotalStorageSize() - getFreeStorageSize()
    } catch (exception: IOException) {
        Log.e("AppStorageManager", "Unable to calculate used storage space")
        0.0
    }

    fun getApkSizeStats(appInfo: ApplicationInfo): ApkSizeStats {
        val storageStats: StorageStats =
            storageStatsManager.queryStatsForUid(appInfo.storageUuid, appInfo.uid)
        return ApkSizeStats(storageStats.cacheBytes, storageStats.dataBytes)
    }

    data class ApkSizeStats(
        val dataSize: Long,
        val cacheSize: Long
    )
}