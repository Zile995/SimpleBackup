package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.FileUtil.getApkFilesInsideDir
import com.stefan.simplebackup.utils.file.FileUtil.getApkZipFile
import com.stefan.simplebackup.utils.file.FileUtil.getBackupDirPath
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
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
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
object ZipUtil {

    suspend fun zipAllData(app: AppData) {
        withContext(ioDispatcher) {
            launch {
                zipApks(app)
            }
            zipTarArchive(app)
        }
    }

    suspend fun extractAllData(app: AppData) {
        withContext(ioDispatcher) {
            extractApks(app)
        }
    }

    @Throws(ZipException::class)
    private suspend fun zipApks(app: AppData) {
        coroutineScope {
            val apkFiles = async { getApkFilesInsideDir(app) }
            val tempDirPath = getTempDirPath(app)
            val zipParameters = getZipParameters()
            val zipFile = ZipFile("$tempDirPath/${app.name}.zip")
            Log.d("ZipUtil", "Zipping the ${app.name} apks to $tempDirPath")
            zipFile.addFiles(apkFiles.await(), zipParameters)
            Log.d("ZipUtil", "Successfully zipped ${app.name} apks")
        }
    }

    private suspend fun extractApks(app: AppData) {
        coroutineScope {
            val backupDirPath = getBackupDirPath(app)
            Log.d("ZipUtil", "Extracting the ${app.name} apks to $backupDirPath")
            val zipFile = getApkZipFile(backupDirPath)
            zipFile?.apply {
                extractAll(backupDirPath)
                Log.d("ZipUtil", "Successfully extracted ${app.name} apks")
            } ?: {
                throw IOException("Unable to find zip file")
            }
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

    suspend fun getAppAbiList(app: AppData) = coroutineScope {
        val abiList = mutableListOf<String>()
        val apkFiles = File(app.apkDir).listFiles()?.filter { file ->
            file.extension == "apk"
        }
        apkFiles?.forEach { apkFile ->
            val apkAbiList = getApkAbiList(apkFile)
            abiList.addAll(apkAbiList)
        }
        abiList.distinct()
    }

    private suspend fun getApkAbiList(apkFile: File) = coroutineScope {
        val abiList = mutableListOf<String>()
        try {
            val zipFile = ZipFile(apkFile)
            val headerList = zipFile.fileHeaders
            abiList.addAll(headerList.map { fileHeader ->
                fileHeader.fileName
            }.filter { fileName ->
                fileName.contains("lib") && fileName.endsWith(".so")
            }.map {
                it.substringAfter("/").substringBeforeLast("/")
            }.distinct())
        } catch (e: IOException) {
            e.message?.let { message ->
                Log.e(
                    "ZipUtil",
                    "${apkFile.name}: $message"
                )
            }
            abiList.clear()
        }
        abiList
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
                compressionLevel =
                    enumValues<CompressionLevel>()[PreferenceHelper.savedZipCompressionLevel.toInt()]
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }
    }
}