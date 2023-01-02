package com.stefan.simplebackup.utils.work

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// Dirs to be excluded
private const val CACHE_DIR_NAME = "cache"
private const val NO_BACKUP_DIR_NAME = "no_backup"
private const val CODE_CACHE_DIR_NAME = "code_cache"

// GMS backup files to be excluded
private const val GMS_APP_ID_FILE_NAME = "shared_prefs/com.google.android.gms.appid.xml"
private const val GMS_MEASUREMENTS_FILE_NAME =
    "shared_prefs/com.google.android.gms.measurement.prefs.xml"

object TarUtil {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(IOException::class)
    suspend fun backupData(app: AppData) {
        withContext(ioDispatcher) {
            if (Shell.isAppGrantedRoot() == false) return@withContext

            // Get exclude commands
            val excludeCommand = getExcludeCommand()

            // Set archive name and path
            val tarArchiveName = getArchiveName(app)
            val tarArchivePath = FileUtil.getTempDirPath(app) + "/$tarArchiveName"

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
            if (Shell.isAppGrantedRoot() == false) return@withContext
            val tarArchiveName = getArchiveName(app)
            val tarArchivePath = FileUtil.getTempDirPath(app) + "/$tarArchiveName"

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
        Shell.cmd("tar -cf $archivePath -C $dataPath $excludeCommand .").exec()

    private fun unarchiveData(archivePath: String, dataPath: String) =
        Shell.cmd("tar -xf $archivePath -C $dataPath").exec()

    private fun getExcludeCommand(): String {
        val shouldExcludeCache = PreferenceHelper.shouldExcludeAppsCache

        // Standard excluded dirs and files
        val standardExcludeNames = "\"$LIB_DIR_NAME\"," +
                "\"$NO_BACKUP_DIR_NAME\"," +
                "\"$GMS_APP_ID_FILE_NAME\"," +
                "\"$GMS_MEASUREMENTS_FILE_NAME\""

        return if (shouldExcludeCache)
            "--exclude={$standardExcludeNames,\"$CACHE_DIR_NAME\",\"$CODE_CACHE_DIR_NAME\"}"
        else
            "--exclude={$standardExcludeNames}"
    }

    fun getOwnerUid(uid: Int): String {
        val stringUid = uid.toString()
        Log.d("TarUtil", "uid is $stringUid")
        val trimmedUid = stringUid.substring(2).run { trimStart('0') }
        val formattedOwnerUid = buildString {
            append("u0_a")
            append(trimmedUid)
        }
        Log.d("TarUtil", "Formatted owner uid is $stringUid")
        return formattedOwnerUid
    }

    private fun restoreAppUid(dataPath: String, uid: Int) {
        val ownerUid = getOwnerUid(uid)
        Shell.cmd("chown $ownerUid:$ownerUid $dataPath -R").exec()
    }

    private fun restoreSELinuxContext(dataPath: String) {
        Shell.cmd("restorecon -R $dataPath").exec()
    }
}