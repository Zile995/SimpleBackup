package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.APK_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.FileUtil.getApkFilesInsideDir
import com.stefan.simplebackup.utils.file.FileUtil.getApkZipFile
import com.stefan.simplebackup.utils.file.FileUtil.getBackupDirPath
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.LIB_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.TAR_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.ZIP_FILE_EXTENSION
import kotlinx.coroutines.*
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

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(ZipException::class)
    suspend fun zipAllData(app: AppData) {
        withContext(ioDispatcher) {
            launch {
                zipApks(app)
            }
            zipTarArchive(app)
        }
    }

    @Throws(ZipException::class)
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
            val zipFile = ZipFile("$tempDirPath/${app.name}.$ZIP_FILE_EXTENSION")
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
            val zipFile = getApkZipFile(appBackupDirPath = backupDirPath)
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
                    "${getTempDirPath(app)}/${app.packageName}.$ZIP_FILE_EXTENSION", "pass123".toCharArray()
                )
                if (zipFile.file.exists()) zipFile.file.delete()
                Log.d("ZipUtil", "Zipping the ${app.packageName} tar archive to $tempDirPath")
                zipFile.addFile(tarArchive, zipParameters)
                tarArchive.delete()
                Log.d("ZipUtil", "Successfully zipped ${app.name} data")
            }
        }
    }

    private fun String.findTarArchive(app: AppData): File? {
        val tarArchive = File("$this/${app.packageName}.$TAR_FILE_EXTENSION")
        return if (tarArchive.exists()) tarArchive else null
    }

    suspend fun getAppAbiList(app: AppData) = withContext(ioDispatcher) {
        File(app.apkDir).run {
            walkTopDown().filter { file ->
                file.extension == APK_FILE_EXTENSION
            }.flatMap { apkFile ->
                getApkAbiList(apkFile)
            }.toList()
        }
    }

    private fun getApkAbiList(apkFile: File) = try {
        val zipFile = ZipFile(apkFile)
        val headerList = zipFile.fileHeaders
        headerList.asSequence().map { fileHeader ->
            fileHeader.fileName
        }.filter { fileName ->
            fileName.contains("lib") && fileName.endsWith(".$LIB_FILE_EXTENSION")
        }.map {
            it.substringAfter(File.separator).substringBeforeLast(File.separator)
        }.distinct()
    } catch (e: IOException) {
        Log.e("ZipUtil", "${apkFile.name}: $e ${e.message}")
        sequenceOf()
    }

    private fun getZipParameters(isApk: Boolean = true): ZipParameters {
        val zipParameters = ZipParameters()
        return if (isApk) {
            zipParameters.apply {
                compressionMethod = CompressionMethod.STORE
            }
        } else {
            zipParameters.apply {
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