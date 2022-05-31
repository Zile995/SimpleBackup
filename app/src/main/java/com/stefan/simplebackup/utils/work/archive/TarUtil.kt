package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.FileHelper
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.withContext
import java.io.IOException

object TarUtil : FileHelper {

    @Throws(IOException::class)
    suspend fun backupData(app: AppData) {
        withContext(ioDispatcher) {
            Log.d("TarUtil", "Creating the ${app.packageName} data archive")
            val tarArchivePath =
                getTempDirPath(app) + "/${app.packageName}.tar"
            val result = Shell.cmd(
                "tar -cf $tarArchivePath" +
                        " --exclude={\"cache\",\"lib\",\"code_cache\"}" +
                        " -C ${app.dataDir} . "
            ).exec()
            when {
                result.isSuccess -> Log.d(
                    "TarUtil",
                    "Successfully created ${app.packageName}.tar data archive"
                )
                else -> {
                    Log.d("TarUtil", "Unable to create data archive")
                }
            }
        }
    }

    suspend fun restoreData(app: AppData) {
        withContext(ioDispatcher) {

        }
    }
}