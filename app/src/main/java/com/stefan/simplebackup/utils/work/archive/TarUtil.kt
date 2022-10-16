package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.TAR_FILE_EXTENSION
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object TarUtil {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(IOException::class)
    suspend fun backupData(app: AppData) {
        withContext(ioDispatcher) {
            val shouldExcludeCache = PreferenceHelper.shouldExcludeAppsCache
            val excludeCommand =
                if (shouldExcludeCache) "--exclude={\"cache\",\"lib\",\"code_cache\"}"
                else "--exclude={\"lib\"}"
            Log.d("TarUtil", "Creating the ${app.packageName} data archive")
            val tarArchiveName = "${app.packageName}.$TAR_FILE_EXTENSION"
            val tarArchivePath = getTempDirPath(app) + "/$tarArchiveName"
            val result = Shell.cmd(
                "tar -cf $tarArchivePath" + " $excludeCommand" + " -C ${app.dataDir} . "
            ).exec()
            when {
                result.isSuccess -> Log.d(
                    "TarUtil", "Successfully created $tarArchiveName data archive"
                )
                else -> {
                    val message = "Unable to create data archive"
                    Log.w("TarUtil", message)
                    throw IOException(message)
                }
            }
        }
    }

    suspend fun restoreData(app: AppData) {
        withContext(ioDispatcher) {

        }
    }
}