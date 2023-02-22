package com.stefan.simplebackup.utils.work

import android.util.Log
import androidx.annotation.WorkerThread
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.data.file.*
import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.IOException

object ZipUtil {

    @WorkerThread
    @Throws(ZipException::class, IOException::class)
    suspend fun zipAllData(app: AppData, dirPath: String) = coroutineScope {
        launch {
            zipApks(app, dirPath)
        }
        zipTarArchive(app, dirPath)
    }

    @WorkerThread
    @Throws(ZipException::class, IOException::class)
    suspend fun unzipAllData(app: AppData, backupDirPath: String) = coroutineScope {
        launch {
            unzipApks(app, backupDirPath)
        }
        unzipTarArchive(app, backupDirPath)
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun zipApks(app: AppData, tempDirPath: String) {
        coroutineScope {
            val apkFiles = FileUtil.getApks(app.apkDir)
            val zipParameters = getZipParameters()
            val zipFile = ZipFile("$tempDirPath/${app.name}.$ZIP_FILE_EXTENSION")
            if (zipFile.file.exists()) zipFile.file.delete()
            Log.d("ZipUtil", "Zipping the ${app.name} apks to $tempDirPath")
            zipFile.addFiles(apkFiles, zipParameters)
            Log.d("ZipUtil", "Successfully zipped ${app.name} apks")
        }
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun unzipApks(app: AppData, backupDirPath: String) {
        coroutineScope {
            Log.d("ZipUtil", "Extracting the ${app.name} apks to temp dir")
            val apkZipFile = getApkZipFile(appBackupDirPath = backupDirPath)
            apkZipFile.apply {
                if (!apkZipFile.isValidZipFile) throw IOException("Apk zip file is not valid")
                val tempDirPath = FileUtil.getTempDirPath(app)
                extractAll(tempDirPath)
                Log.d("ZipUtil", "Successfully extracted ${app.name} apks")
            }
        }
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun zipTarArchive(app: AppData, tempDirPAth: String) = coroutineScope {
        val zipParameters = getZipParameters(isApk = false)
        val tarArchive = FileUtil.getTarArchive(dirPath = tempDirPAth, app = app)
        // Save data size to app
        app.dataSize += tarArchive.length()
        val zipFile =
            ZipFile("$tempDirPAth/${app.packageName}.$ZIP_FILE_EXTENSION")
        if (zipFile.file.exists()) zipFile.file.delete()
        Log.d("ZipUtil", "Zipping the ${app.packageName} tar archive to $tempDirPAth")
        zipFile.addFile(tarArchive, zipParameters)
        tarArchive.delete()
        Log.d("ZipUtil", "Successfully zipped ${app.name} data")
    }

    @Throws(ZipException::class, IOException::class)
    private suspend fun unzipTarArchive(app: AppData, backupDirPath: String) {
        coroutineScope {
            Log.d("ZipUtil", "Extracting the ${app.name} tar to temp dir")
            val tarZipFile = getTarZipFile(appBackupDirPath = backupDirPath)
            tarZipFile.apply {
                if (!tarZipFile.isValidZipFile) throw IOException("Tar zip file is not valid")
                val tempDirPath = FileUtil.getTempDirPath(app)
                extractAll(tempDirPath)
                Log.d("ZipUtil", "Successfully extracted ${app.packageName} tar")
            }
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    suspend fun getTarZipFile(appBackupDirPath: String) = coroutineScope {
        File(appBackupDirPath).walkTopDown().filter { backupFile ->
            backupFile.isFile && backupFile.extension == ZIP_FILE_EXTENSION
        }.map { fileWithZipExtension ->
            ZipFile(fileWithZipExtension)
        }.firstOrNull { zipFile ->
            zipFile.fileHeaders.map { fileHeader ->
                fileHeader.fileName.endsWith(".$TAR_FILE_EXTENSION")
            }.all { it }
        } ?: throw IOException("Unable to find tar zip file")
    }

    @WorkerThread
    @Throws(IOException::class)
    suspend fun getApkZipFile(appBackupDirPath: String) = coroutineScope {
        File(appBackupDirPath).walkTopDown().filter { backupFile ->
            backupFile.isFile && backupFile.extension == ZIP_FILE_EXTENSION
        }.map { fileWithZipExtension ->
            ZipFile(fileWithZipExtension)
        }.firstOrNull { zipFile ->
            zipFile.fileHeaders.map { fileHeader ->
                fileHeader.fileName.endsWith(".$APK_FILE_EXTENSION")
            }.all { fileNameHasApkExtension -> fileNameHasApkExtension }
        } ?: throw IOException("Unable to find apk zip file")
    }

    @WorkerThread
    suspend fun getNativeLibs(app: AppData) = coroutineScope {
        File(app.apkDir)
            .walkTopDown()
            .filter { file ->
                file.extension == APK_FILE_EXTENSION
            }.flatMap { apkFile ->
                getApkNativeLibs(apkFile)
            }.distinct().toList()
    }

    private fun getApkNativeLibs(apkFile: File) = try {
        val zipFile = ZipFile(apkFile)
        val headerList = zipFile.fileHeaders
        headerList.asSequence().map { fileHeader ->
            fileHeader.fileName
        }.filter { fileName ->
            fileName.contains(LIB_DIR_NAME) && fileName.endsWith(".$LIB_FILE_EXTENSION")
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
