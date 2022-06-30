package com.stefan.simplebackup.utils.work.restore

import android.content.Context
import com.stefan.simplebackup.MainApplication.Companion.getDatabaseInstance
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.work.archive.ZipUtil
import kotlinx.coroutines.coroutineScope

private const val TMP: String = "/data/local/tmp"
private const val DATA: String = "/data/data"

class RestoreUtil(
    private val appContext: Context,
    private val restoreItems: IntArray
) {

    private var currentProgress = 0
    private val updateProgress: () -> Unit = {
        currentProgress += (PROGRESS_MAX / restoreItems.size) / 3
    }

    suspend fun restore() = coroutineScope {
        val database = appContext.getDatabaseInstance(this)
        val repository = AppRepository(database.appDao())
        restoreItems.forEach { item ->
            repository.getAppData(item).also { app ->
                ZipUtil.extractAllData(app)
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