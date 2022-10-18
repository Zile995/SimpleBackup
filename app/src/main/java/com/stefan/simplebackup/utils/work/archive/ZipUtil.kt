package com.stefan.simplebackup.utils.work.archive

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.file.APK_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.FileUtil.findTarArchive
import com.stefan.simplebackup.utils.file.FileUtil.getApkFilesInsideDir
import com.stefan.simplebackup.utils.file.FileUtil.getBackupDirPath
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.LIB_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.TAR_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.ZIP_FILE_EXTENSION
import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
object ZipUtil {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(ZipException::class, IOException::class)
    suspend fun zipAllData(app: AppData) {
        withContext(ioDispatcher) {
            launch {
                zipApks(app)
            }
            zipTarArchive(app)
        }
    }

    @Throws(ZipException::class, IOException::class)
    suspend fun unzipAllData(app: AppData) {
        withContext(ioDispatcher) {
            launch {
                unzipApks(app)
            }
            unzipTarArchive(app)
        }
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun zipApks(app: AppData) {
        coroutineScope {
            val apkFiles = async { getApkFilesInsideDir(app) }
            val tempDirPath = getTempDirPath(app)
            val zipParameters = getZipParameters()
            val zipFile = ZipFile("$tempDirPath/${app.name}.$ZIP_FILE_EXTENSION")
            if (zipFile.file.exists()) zipFile.file.delete()
            Log.d("ZipUtil", "Zipping the ${app.name} apks to $tempDirPath")
            zipFile.addFiles(apkFiles.await(), zipParameters)
            Log.d("ZipUtil", "Successfully zipped ${app.name} apks")
        }
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun unzipTarArchive(app: AppData) {
        coroutineScope {
            val backupDirPath = getBackupDirPath(app)
            Log.d("ZipUtil", "Extracting the ${app.name} tar to temp dir")
            val zipFile = getTarZipFile(appBackupDirPath = backupDirPath)
            zipFile?.apply {
                val tempDirPath = getTempDirPath(app)
                extractAll(tempDirPath)
                Log.d("ZipUtil", "Successfully extracted ${app.packageName} tar")
            } ?: {
                throw IOException("Unable to find zip tar file")
            }
        }
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun unzipApks(app: AppData) {
        coroutineScope {
            val backupDirPath = getBackupDirPath(app)
            Log.d("ZipUtil", "Extracting the ${app.name} apks to temp dir")
            val apkZipFile = getApkZipFile(appBackupDirPath = backupDirPath)
            apkZipFile?.apply {
                val tempDirPath = getTempDirPath(app)
                extractAll(tempDirPath)
                Log.d("ZipUtil", "Successfully extracted ${app.name} apks")
            } ?: {
                throw IOException("Unable to find apk zip file")
            }
        }
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun zipTarArchive(app: AppData) {
        coroutineScope {
            val tempDirPath = getTempDirPath(app)
            val zipParameters = getZipParameters(isApk = false)
            findTarArchive(dirPath = tempDirPath, app = app).let { tarArchive ->
                // Save dataSize to app
                app.dataSize += tarArchive.length()
                val zipFile = ZipFile(
                    "${getTempDirPath(app)}/${app.packageName}.$ZIP_FILE_EXTENSION")
                if (zipFile.file.exists()) zipFile.file.delete()
                Log.d("ZipUtil", "Zipping the ${app.packageName} tar archive to $tempDirPath")
                zipFile.addFile(tarArchive, zipParameters)
                tarArchive.delete()
                Log.d("ZipUtil", "Successfully zipped ${app.name} data")
            }
        }
    }

    suspend fun getAppNativeLibs(app: AppData) = withContext(ioDispatcher) {
        File(app.apkDir).run {
            walkTopDown().filter { file ->
                file.extension == APK_FILE_EXTENSION
            }.flatMap { apkFile ->
                getApkNativeLibs(apkFile)
            }.toList()
        }
    }

    private fun getTarZipFile(appBackupDirPath: String) =
        File(appBackupDirPath).walkTopDown().filter { backupFile ->
            backupFile.isFile && backupFile.extension == ZIP_FILE_EXTENSION
        }.map { fileWithZipExtension ->
            ZipFile(fileWithZipExtension)
        }.firstOrNull { zipFile ->
            zipFile.fileHeaders.map { fileHeader ->
                fileHeader.fileName.endsWith(".$TAR_FILE_EXTENSION")
            }.all { fileNameHasTarExtension -> fileNameHasTarExtension }
        }

    private fun getApkZipFile(appBackupDirPath: String) =
        File(appBackupDirPath).walkTopDown().filter { backupFile ->
            backupFile.isFile && backupFile.extension == ZIP_FILE_EXTENSION
        }.map { fileWithZipExtension ->
            ZipFile(fileWithZipExtension)
        }.firstOrNull { zipFile ->
            zipFile.fileHeaders.map { fileHeader ->
                fileHeader.fileName.endsWith(".$APK_FILE_EXTENSION")
            }.all { fileNameHasApkExtension -> fileNameHasApkExtension }
        }

    private fun getApkNativeLibs(apkFile: File) = try {
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
        Log.e("ZipUtil", "${apkFile.name}: $e")
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
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel =
                    enumValues<CompressionLevel>()[PreferenceHelper.savedZipCompressionLevel.toInt()]
            }
        }
    }
}