package com.stefan.simplebackup.utils.restore

import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.main.PreferenceHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.withContext

private const val TMP: String = "/data/local/tmp"
private const val DATA: String = "/data/data"

class RestoreUtil(
    private val packageNames: IntArray
) : FileHelper {

    private var currentProgress = 0
    private val updatedProgress: Int
        get() {
            currentProgress += (PROGRESS_MAX / packageNames.size) / 3
            return currentProgress
        }

    suspend fun restore() {
        withContext(ioDispatcher) {
            PreferenceHelper.savePackageName(null)
            unzipData()
            restoreData()
        }
    }

    private fun unzipData() {

    }

    private fun restoreData() {

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