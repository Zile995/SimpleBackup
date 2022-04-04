package com.stefan.simplebackup.utils.restore

import com.stefan.simplebackup.utils.backup.BackupHelper
import android.content.Context
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.utils.main.TarUtil
import com.stefan.simplebackup.utils.main.ZipUtil

private const val TMP: String = "/data/local/tmp"
private const val DATA: String = "/data/data"

class RestoreUtil (
    appContext: Context,
    app: AppData
): BackupHelper(appContext) {

    private val zipUtil = ZipUtil(appContext, app)
    private val tarUtil = TarUtil(appContext, app)

    suspend fun restore() {

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