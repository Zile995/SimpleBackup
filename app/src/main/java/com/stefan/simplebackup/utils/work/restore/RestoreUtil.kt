package com.stefan.simplebackup.utils.work.restore

import android.content.Context
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.utils.work.WorkResult
import com.stefan.simplebackup.utils.work.WorkUtil
import kotlinx.coroutines.coroutineScope

class RestoreUtil(
    appContext: Context,
    private val restoreItems: Array<String>,
    updateForegroundInfo: ForegroundCallback
) : WorkUtil(appContext, restoreItems, updateForegroundInfo) {

    suspend fun restore() = coroutineScope {
        restoreItems.forEach { item ->

        }
    }

    override fun updateWhenAppDoesNotExists(): WorkResult {
        TODO("Not yet implemented")
    }

    override suspend fun AppData.updateOnSuccess(): WorkResult {
        TODO("Not yet implemented")
    }

    override suspend fun AppData.updateOnFailure(): WorkResult {
        TODO("Not yet implemented")
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