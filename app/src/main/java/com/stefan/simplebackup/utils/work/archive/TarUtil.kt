package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.LIB_DIR_NAME
import com.stefan.simplebackup.utils.file.TAR_FILE_EXTENSION
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

const val CACHE_PRIVATE_DIR_NAME = "cache"
const val CODE_CACHE_PRIVATE_DIR_NAME = "code_cache"

object TarUtil {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(IOException::class)
    suspend fun backupData(app: AppData) {
        withContext(ioDispatcher) {
            // Get exclude commands
            val excludeCommand = getExcludeCommand()

            // Set archive name and path
            val tarArchiveName = getArchiveName(app)
            val tarArchivePath = getTempDirPath(app) + "/$tarArchiveName"

            val tarArchiveFile = File(tarArchivePath)
            if (tarArchiveFile.exists()) tarArchiveFile.delete()

            Log.d("TarUtil", "Creating the ${app.packageName} data archive")
            val result = archiveData(
                archivePath = tarArchivePath,
                excludeCommand = excludeCommand,
                dataPath = app.dataDir
            )
            if (result.isSuccess)
                Log.d("TarUtil", "Successfully created $tarArchiveName data archive")
            else {
                val message = "Unable to create data archive"
                Log.w("TarUtil", message)
                if (Shell.isAppGrantedRoot() == true)
                    throw IOException(message)
                else
                    Log.w("TarUtil", "App doesn't have root access, unable to backup data")
            }
        }
    }

    @Throws(IOException::class)
    suspend fun restoreData(app: AppData, uid: Int) {
        withContext(ioDispatcher) {
            val tarArchiveName = getArchiveName(app)
            val tarArchivePath = getTempDirPath(app) + "/$tarArchiveName"

            Log.d("TarUtil", "Unarchiving the ${app.packageName} tar archive")
            val result = unarchiveData(
                archivePath = tarArchivePath,
                dataPath = app.dataDir
            )
            Log.d("TarUtil", "Restoring the ${app.packageName} uid")
            restoreAppUid(dataPath = app.dataDir, uid = uid)
            Log.d("TarUtil", "Restoring the ${app.packageName} SELinux context")
            restoreSELinuxContext(dataPath = app.dataDir)
            if (result.isSuccess)
                Log.d("TarUtil", "Successfully restored $tarArchiveName data archive")
            else {
                val message = "Unable to restore data"
                Log.w("TarUtil", message)
                if (Shell.isAppGrantedRoot() == true)
                    throw IOException(message)
                else
                    Log.w("TarUtil", "App doesn't have root access, unable to restore data")
            }
        }
    }

    private fun getArchiveName(app: AppData) = "${app.packageName}.$TAR_FILE_EXTENSION"

    private fun archiveData(archivePath: String, excludeCommand: String, dataPath: String) =
        Shell.cmd("tar -cf $archivePath $excludeCommand -C $dataPath . ").exec()

    private fun unarchiveData(archivePath: String, dataPath: String) =
        Shell.cmd("tar -xf $archivePath -C $dataPath").exec()

    private fun getExcludeCommand(): String {
        val shouldExcludeCache = PreferenceHelper.shouldExcludeAppsCache
        return if (shouldExcludeCache)
            "--exclude={\"$CACHE_PRIVATE_DIR_NAME\",\"$LIB_DIR_NAME\",\"$CODE_CACHE_PRIVATE_DIR_NAME\"}"
        else
            "--exclude={\"$LIB_DIR_NAME\"}"
    }

    private fun restoreAppUid(dataPath: String, uid: Int) {
        val stringUid = uid.toString()
        val formattedUid = stringUid.substring(2).run {
            trimStart('0')
        }
        val ownerUid = "u0_a$formattedUid"
        Shell.cmd("chown $ownerUid:$ownerUid $dataPath -R").exec()
    }

    private fun restoreSELinuxContext(dataPath: String) {
        Shell.cmd("restorecon -R $dataPath").exec()
    }

}