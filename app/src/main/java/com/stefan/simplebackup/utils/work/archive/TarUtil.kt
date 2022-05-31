package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.FileHelper
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.withContext
import java.io.IOException

object TarUtil : FileHelper {
    suspend fun backupData(app: AppData) {
        withContext(ioDispatcher) {
            try {
                val isGranted = Shell.isAppGrantedRoot() ?: return@withContext
                if (!isGranted) {
                    return@withContext
                }
                Log.d("TarUtil", "Creating the ${app.packageName} data archive")
                val tarArchivePath =
                    getBackupDirPath(app) + "/${app.packageName}.tar"
                Shell.su(
                    "tar -cf $tarArchivePath" +
                            " --exclude={\"cache\",\"lib\",\"code_cache\"}" +
                            " -C ${app.dataDir} . "
                ).exec()
                Log.d("TarUtil", "Successfully created ${app.packageName}.tar data archive")
            } catch (e: IOException) {
                e.message?.let { message ->
                    Log.e("TarUtil", message)
                }
            }
        }
    }

    suspend fun restoreData(app: AppData) {
        withContext(ioDispatcher) {

        }
    }

}