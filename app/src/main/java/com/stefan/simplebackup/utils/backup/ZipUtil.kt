package com.stefan.simplebackup.utils.backup

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

class ZipUtil(
    context: Context,
    private val appList: MutableList<AppData>
) : BackupHelper(context) {

    suspend fun zipAllData() {
        appList.forEach { backupApp ->
            withContext(ioDispatcher) {
                launch { zipApks(backupApp, getBackupDirPath(backupApp)) }
                launch { zipTarArchive(backupApp, getBackupDirPath(backupApp)) }
            }
        }
    }

    private suspend fun zipTarArchive(app: AppData, backupDirPath: String) {
        runCatching {
            val zipParameters = getZipParameters(false)
            getTarArchive(app)?.let { tarArchive ->
                val zipFile = ZipFile(
                    "${backupDirPath}/${app.getPackageName()}.zip",
                    "pass123".toCharArray()
                )
                zipFile.addFile(tarArchive, zipParameters)
                serializeApp(app, backupDirPath)
                tarArchive.delete()
            }
        }.onFailure { throwable ->
            throwable.message?.let { message ->
                Log.e("ZipUtil", message)
            }
        }
    }

    private fun zipApks(app: AppData, backupDirPath: String) {
        runCatching {
            val zipParameters = getZipParameters(true)
            Log.d("ZipUtil", "Zipping the ${app.getName()} apks to $backupDirPath")
            val zipFile = ZipFile(backupDirPath + "/${app.getName()}.zip")
            if (zipFile.file.exists()) {
                zipFile.file.delete()
            }
            zipFile.addFiles(getApkList(app), zipParameters)
        }.onFailure { throwable ->
            throwable.message?.let { message ->
                Log.e("ZipUtil", message)
            }
        }
    }

    private fun getTarArchive(app: AppData): File? {
        var containerFile: File? = null
        val backupDir = File(getBackupDirPath(app))
        backupDir.listFiles()?.filter {
            it.isFile && it.extension == "tar"
        }?.map {
            containerFile = it
        }
        return containerFile
    }

    private fun getApkList(app: AppData): MutableList<File> {
        val apkList = mutableListOf<File>()
        Log.d("ZipUtil", "Found the ${app.getName()} apk dir: ${app.getApkDir()}")
        val dir = File(app.getApkDir())
        dir.walkTopDown().filter {
            it.extension == "apk"
        }.forEach { apk ->
            apkList.add(apk)
        }
        Log.d("ZipUtil", "Got the apk list for ${app.getName()}: ${apkList.map { it.name }}")
        return apkList
    }

    private fun getZipParameters(isApk: Boolean): ZipParameters {
        return if (isApk) {
            ZipParameters().apply {
                compressionMethod = CompressionMethod.STORE
            }
        } else {
            ZipParameters().apply {
                isEncryptFiles = true
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.FASTEST
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }
    }
}