package com.stefan.simplebackup.utils.main

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.utils.backup.BackupHelper
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
            val zipParameters = getZipParameters(isApk = false)
            findTarArchive()?.let { tarArchive ->
                val zipFile = ZipFile(
                    "${backupDirPath}/${app.packageName}.zip",
                    "pass123".toCharArray()
                )
                Log.d(
                    "ZipUtil",
                    "Zipping the ${app.packageName} tar archive to $backupDirPath"
                )
                zipFile.addFile(tarArchive, zipParameters)
                tarArchive.delete()
            }
        }.onSuccess {
            Log.d("ZipUtil", "Successfully zipped ${app.name} data")
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
            val zipParameters = getZipParameters(isApk = true)
            val zipFile = ZipFile(backupDirPath + "/${app.name}.zip")
            if (zipFile.file.exists()) {
                zipFile.file.delete()
            }
            Log.d("ZipUtil", "Zipping the ${app.name} apks to $backupDirPath")
            zipFile.addFiles(getApkList(app), zipParameters)
        }.onSuccess {
            Log.d("ZipUtil", "Successfully zipped ${app.name} apks")
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

    private fun findTarArchive(): File? {
        val tarArchive = File(
            backupDirPath +
                    "/" +
                    app.packageName +
                    ".tar"
        )
        return if (tarArchive.exists()) tarArchive else null
    }

    private fun getApkList(app: AppData): MutableList<File> {
        val apkList = mutableListOf<File>()
        Log.d("ZipUtil", "Found the ${app.name} apk dir: ${app.apkDir}")
        val dir = File(app.apkDir)
        dir.walkTopDown().filter {
            it.extension == "apk"
        }.forEach { apk ->
            apkList.add(apk)
        }
        Log.d("ZipUtil", "Got the apk list for ${app.name}: ${apkList.map { it.name }}")
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