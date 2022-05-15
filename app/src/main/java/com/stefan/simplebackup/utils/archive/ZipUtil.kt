package com.stefan.simplebackup.utils.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
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

object ZipUtil : FileHelper {

    suspend fun zipAllData(app: AppData) {
        withContext(ioDispatcher) {
            launch {
                zipApks(app)
            }
            zipTarArchive(app)
        }
    }

    private fun zipApks(app: AppData) {
        try {
            val backupDirPath = getBackupDirPath(app)
            val zipParameters = getZipParameters()
            val zipFile = ZipFile("$backupDirPath/${app.name}.zip")
            if (zipFile.file.exists()) {
                zipFile.file.delete()
            }
            Log.d("ZipUtil", "Zipping the ${app.name} apks to $backupDirPath")
            zipFile.addFiles(getApkList(app), zipParameters)
            Log.d("ZipUtil", "Successfully zipped ${app.name} apks")
        } catch (exception: ZipException) {
            exception.message?.let { message ->
                Log.e("ZipUtil", message)
            }
        }
    }

    private fun zipTarArchive(app: AppData) {
        try {
            val backupDirPath = getBackupDirPath(app)
            val zipParameters = getZipParameters(isApk = false)
            backupDirPath.findTarArchive(app)?.let { tarArchive ->
                val zipFile = ZipFile(
                    "${getBackupDirPath(app)}/${app.packageName}.zip",
                    "pass123".toCharArray()
                )
                Log.d(
                    "ZipUtil",
                    "Zipping the ${app.packageName} tar archive to $backupDirPath"
                )
                zipFile.addFile(tarArchive, zipParameters)
                tarArchive.delete()
                Log.d("ZipUtil", "Successfully zipped ${app.name} data")
            }
        } catch (exception: ZipException) {
            exception.message?.let { message ->
                Log.e("ZipUtil", message)
            }
        }
    }

    private fun String.findTarArchive(app: AppData): File? {
        val tarArchive = File("$this/${app.packageName}.tar")
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

    private fun getZipParameters(isApk: Boolean = true): ZipParameters {
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