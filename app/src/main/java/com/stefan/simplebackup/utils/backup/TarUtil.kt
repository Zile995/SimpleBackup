package com.stefan.simplebackup.utils.backup

import android.content.Context
import com.stefan.simplebackup.data.AppData
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.withContext

class TarUtil(context: Context, private val appList: MutableList<AppData>) : BackupHelper(context) {

    suspend fun backupData() {
        withContext(ioDispatcher) {
            if (Shell.rootAccess()) {
                appList.forEach { backupApp ->
                    val tarArchivePath =
                        getBackupDirPath(backupApp) + "/${backupApp.getPackageName()}.tar"
                    Shell.su("tar -zcf $tarArchivePath --exclude={\"cache\",\"lib\",\"code_cache\"} -C ${backupApp.getDataDir()} . ")
                        .exec()
                }
            }
        }
    }
}