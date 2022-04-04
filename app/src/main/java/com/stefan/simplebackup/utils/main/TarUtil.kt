package com.stefan.simplebackup.utils.main

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.utils.backup.BackupHelper
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.withContext
import java.io.IOException

class TarUtil(context: Context, var app: AppData) : BackupHelper(context) {

    suspend fun backupData() {
        withContext(ioDispatcher) {
            runCatching {
                if (!Shell.rootAccess()) {
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
            }.onSuccess {
                Log.d("TarUtil", "Successfully created ${app.packageName}.tar data archive")
            }.onFailure { throwable ->
                when (throwable) {
                    is IOException -> {
                        throwable.message?.let { message ->
                            Log.e("TarUtil", message)
                        }
                    }
                    else -> {
                        throw throwable
                    }
                }
            }
        }
    }

    suspend fun restoreData() {
        withContext(ioDispatcher) {

        }
    }

}