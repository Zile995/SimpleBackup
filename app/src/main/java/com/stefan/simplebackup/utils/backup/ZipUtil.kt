package com.stefan.simplebackup.utils.backup

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

class ZipUtil(
    context: Context,
    private val app: AppData
) : BackupHelper(context) {

    private val backupDirPath = getBackupDirPath(app)

    suspend fun zipAllData() {
        withContext(ioDispatcher) {
            launch { zipApks() }
            launch { zipTarArchive() }
        }
    }

    private fun zipTarArchive() {
        runCatching {
            val zipParameters = getZipParameters(false)
            getTarArchive(app)?.let { tarArchive ->
                val zipFile = ZipFile(
                    "${backupDirPath}/${app.getPackageName()}.zip",
                    "pass123".toCharArray()
                )
                Log.d("ZipUtil", "Zipping the ${app.getPackageName()} tar archive to $backupDirPath")
                zipFile.addFile(tarArchive, zipParameters)
                tarArchive.delete()
            }
        }.onSuccess {
            Log.d("ZipUtil", "Successfully zipped ${app.getName()} data")
        }.onFailure { throwable ->
            when (throwable) {
                is ZipException -> {
                    throwable.message?.let { message ->
                        Log.e("ZipUtil", message)
                    }
                }
                else -> {
                    throw throwable
                }
            }
        }
    }

    private fun zipApks() {
        runCatching {
            val zipParameters = getZipParameters(true)
            val zipFile = ZipFile(backupDirPath + "/${app.getName()}.zip")
            if (zipFile.file.exists()) {
                zipFile.file.delete()
            }
            Log.d("ZipUtil", "Zipping the ${app.getName()} apks to $backupDirPath")
            zipFile.addFiles(getApkList(app), zipParameters)
        }.onSuccess {
            Log.d("ZipUtil", "Successfully zipped ${app.getName()} apks")
        }.onFailure { throwable ->
            when (throwable) {
                is ZipException -> {
                    throwable.message?.let { message ->
                        Log.e("ZipUtil", message)
                    }
                }
                else -> {
                    throw throwable
                }
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