package com.stefan.simplebackup.utils.restore

import android.content.Context
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.archive.TarUtil
import com.stefan.simplebackup.utils.archive.ZipUtil
import com.stefan.simplebackup.utils.backup.BackupHelper
import com.stefan.simplebackup.utils.main.PreferenceHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.withContext

private const val TMP: String = "/data/local/tmp"
private const val DATA: String = "/data/data"

@SuppressWarnings("unused")
class RestoreUtil(
    appContext: Context,
    private val packageNames: Array<String>
) : BackupHelper(appContext) {

    private var zipUtil: ZipUtil? = null
    private var tarUtil: TarUtil? = null

    private var currentProgress = 0
    private val updatedProgress: Int
        get() {
            currentProgress += (PROGRESS_MAX / packageNames.size) / 3
            return currentProgress
        }


    suspend fun restore() {
        savePackageNameToPreferences(TMP + DATA)
        restoreData()
    }


    private suspend fun restoreData() {
        withContext(ioDispatcher) {

        }
    }

    private suspend fun savePackageNameToPreferences(packageName: String) {
        withContext(ioDispatcher) {
            PreferenceHelper.savePackageName(packageName)
        }
    }

}

//private suspend fun installApp(context: Context, app: AppData) {
//        withContext(Dispatchers.IO) {
//            val internalStoragePath = (context.getExternalFilesDir(null)!!.absolutePath).run {
//                substring(0, indexOf("Android")).plus(
//                    ROOT
//                )
//            }
//            println(internalStoragePath)
//            val backupDir = app.dataDir
//            val tempDir = LOCAL.plus(backupDir.removePrefix(internalStoragePath))
//            println(tempDir)
//            val packageName = app.packageName
//            val packageDataDir = "$DATA/$packageName"
//            try {
//                with(Installer) {
//                    // TODO: To be fixed.
//                    Shell.su("x=$(echo -e \"$tempDir\") && mkdir -p \"\$x\"").exec()
//                    Shell.su("x=$(echo -e \"$backupDir/${app.packageName}.tar\")" +
//                            " && y=$(echo -e \"$tempDir/\")" +
//                            " && tar -zxf \"\$x\" -C \"\$y\"").exec()
//                    Shell.su("rm -rf $packageDataDir/*").exec()
//                    Shell.su("restorecon -R $packageDataDir").exec()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }