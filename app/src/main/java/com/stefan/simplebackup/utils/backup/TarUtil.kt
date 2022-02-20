package com.stefan.simplebackup.utils.backup

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.AppData
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.withContext
import java.io.IOException

class TarUtil(context: Context, private val app: AppData) : BackupHelper(context) {

    suspend fun backupData() {
        withContext(ioDispatcher) {
            runCatching {
                if (Shell.rootAccess()) {
                    Log.d("TarUtil", "Creating the ${app.getName()} data archive")
                    val tarArchivePath =
                        getBackupDirPath(app) + "/${app.getPackageName()}.tar"
                    Shell.su("tar -cf $tarArchivePath --exclude={\"cache\",\"lib\",\"code_cache\"} -C ${app.getDataDir()} . ")
                        .exec()
                }
            }.onSuccess {
                Log.d("TarUtil", "Successfully created ${app.getPackageName()}.tar data archive")
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
}