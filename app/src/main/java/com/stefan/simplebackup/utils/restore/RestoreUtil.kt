package com.stefan.simplebackup.utils.restore

import android.content.Context
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.archive.ZipUtil
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.main.PreferenceHelper

private const val TMP: String = "/data/local/tmp"
private const val DATA: String = "/data/data"

class RestoreUtil(
    appContext: Context,
    private val restoreItems: IntArray
) : FileHelper {

    private val repository = (appContext as MainApplication).getRepository

    private var currentProgress = 0
    private val updateProgress: () -> Unit = {
        currentProgress += (PROGRESS_MAX / restoreItems.size) / 3
    }

    suspend fun restore() {
        restoreItems.forEach { item ->
            repository.getAppData(item).also { app ->
                PreferenceHelper.savePackageName(null)
                ZipUtil.extractData(app)
            }
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