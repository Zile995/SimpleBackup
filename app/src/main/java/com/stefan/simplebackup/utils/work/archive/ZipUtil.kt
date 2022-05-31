package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.FileHelper
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.getApkZipFile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

@Suppress("BlockingMethodInNonBlockingContext")
object ZipUtil : FileHelper {

    suspend fun zipAllData(app: AppData) {
        withContext(ioDispatcher) {
            launch {
                zipApks(app)
            }
            zipTarArchive(app)
        }
    }

    suspend fun extractData(app: AppData) {
        withContext(ioDispatcher) {
            extractApks(app)
        }
    }

    @Throws(ZipException::class)
    private suspend fun zipApks(app: AppData) {
        coroutineScope {
            val apkFiles = async { getApkList(app) }
            val tempDirPath = getTempDirPath(app)
            val zipParameters = getZipParameters()
            val zipFile = ZipFile("$tempDirPath/${app.name}.zip")
            if (zipFile.isValidZipFile) {
                return@coroutineScope
            } else {
                FileUtil.deleteFile(zipFile.file.absolutePath)
            }
            Log.d("ZipUtil", "Zipping the ${app.name} apks to $tempDirPath")
            zipFile.addFiles(apkFiles.await(), zipParameters)
            Log.d("ZipUtil", "Successfully zipped ${app.name} apks")
        }
    }

    @Throws(ZipException::class)
    private suspend fun extractApks(app: AppData) {
        coroutineScope {
            val backupDirPath = getBackupDirPath(app)
            Log.d("ZipUtil", "Extracting the ${app.name} apks to $backupDirPath")
            val zipFile = async { getApkZipFile(backupDirPath) }
            zipFile.await()?.apply {
                extractAll(backupDirPath)
                Log.d("ZipUtil", "Successfully extracted ${app.name} apks")
            } ?: Log.d("ZipUtil", "Unable to find zip file")
        }
    }

    @Throws(ZipException::class)
    private suspend fun zipTarArchive(app: AppData) {
        coroutineScope {
            val tempDirPath = getTempDirPath(app)
            val zipParameters = getZipParameters(isApk = false)
            tempDirPath.findTarArchive(app)?.let { tarArchive ->
                val zipFile = ZipFile(
                    "${getTempDirPath(app)}/${app.packageName}.zip",
                    "pass123".toCharArray()
                )
                if (zipFile.file.exists()) zipFile.file.delete()
                Log.d(
                    "ZipUtil",
                    "Zipping the ${app.packageName} tar archive to $tempDirPath"
                )
                zipFile.addFile(tarArchive, zipParameters)
                tarArchive.delete()
                Log.d("ZipUtil", "Successfully zipped ${app.name} data")
            }
        }
    }

    private fun String.findTarArchive(app: AppData): File? {
        val tarArchive = File("$this/${app.packageName}.tar")
        return if (tarArchive.exists()) tarArchive else null
    }

    private fun getApkList(app: AppData): MutableList<File> {
        val apkList = mutableListOf<File>()
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