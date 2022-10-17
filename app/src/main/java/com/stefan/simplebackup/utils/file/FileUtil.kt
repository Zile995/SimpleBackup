package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.IOException
import kotlin.io.path.moveTo

const val LIB_FILE_EXTENSION: String = "so"
const val APK_FILE_EXTENSION: String = "apk"
const val TAR_FILE_EXTENSION: String = "tar"
const val ZIP_FILE_EXTENSION: String = "zip"
const val JSON_FILE_EXTENSION: String = "json"
private const val TEMP_DIR_NAME: String = "temp"
private const val CLOUD_DIR_NAME: String = "cloud"
private const val LOCAL_DIR_NAME: String = "local"

@Suppress("BlockingMethodInNonBlockingContext")
object FileUtil {

    private val ioDispatcher = Dispatchers.IO
    val localDirPath get() = "${mainBackupDirPath}/$LOCAL_DIR_NAME"
    private val tempDirPath get() = "${mainBackupDirPath}/$TEMP_DIR_NAME"
    private val cloudDirPath get() = "${mainBackupDirPath}/$CLOUD_DIR_NAME"

    suspend fun createDirectory(path: String) {
        withContext(ioDispatcher) {
            val dir = File(path)
            if (!dir.exists()) {
                Log.d("FileUtil", "Creating the $path dir")
                dir.mkdirs()
            }
        }
    }

    @Throws(IOException::class)
    suspend fun deleteFile(path: String) {
        withContext(ioDispatcher) {
            val file = File(path)
            if (!file.exists()) throw IOException("File or directory doesn't exist")
            Log.d("FileUtil", "Deleting the $path")
            if (file.isDirectory) {
                val isDeletionSuccessful = file.deleteRecursively()
                if (!isDeletionSuccessful) throw IOException("Unable to delete all files")
                else return@withContext
            } else {
                file.delete()
            }
        }
    }

    @Throws(IOException::class)
    suspend fun moveFiles(sourceDir: File, targetFile: File) {
        if (!sourceDir.isDirectory) throw IOException("File must be verified to be directory beforehand")
        withContext(ioDispatcher) {
            sourceDir.walkTopDown().map { file ->
                file.toPath()
            }.forEach { filePath ->
                filePath.moveTo(target = targetFile.toPath(), overwrite = true)
            }
        }
    }

    @Throws(IOException::class)
    suspend fun deleteDirectoryFiles(dir: File) {
        if (!dir.isDirectory) throw IOException("File must be verified to be directory beforehand")
        withContext(ioDispatcher) {
            dir.walkTopDown().forEach { dirFile ->
                deleteFile(dirFile.absolutePath)
            }
        }
    }

    suspend fun createLocalDir() = createDirectory(localDirPath)

    @Throws(IOException::class)
    suspend fun deleteLocalBackup(packageName: String) = deleteFile("$localDirPath/$packageName")

    fun getBackupDirPath(app: AppData): String = "$localDirPath/${app.packageName}"

    fun getTempDirPath(app: AppData): String = "$tempDirPath/${app.packageName}"

    fun findTarArchive(dirPath: String, app: AppData): File {
        val tarArchive = File("$dirPath/${app.packageName}.$TAR_FILE_EXTENSION")
        if (!tarArchive.exists()) throw IOException("Unable to find tar archive")
        return tarArchive
    }

    suspend fun findFirstJsonInDir(jsonDirPath: String) = withContext(ioDispatcher) {
        File(jsonDirPath).walkTopDown().firstOrNull { dirFile ->
            dirFile.isFile && dirFile.extension == JSON_FILE_EXTENSION
        }
    }

    fun findJsonFilesRecursively(jsonDirPath: String) = flow {
        File(jsonDirPath).walkTopDown().filter { dirFile ->
            dirFile.isFile && dirFile.extension == JSON_FILE_EXTENSION
        }.forEach { jsonFile ->
            emit(jsonFile)
        }
    }.flowOn(ioDispatcher)

    fun getApkFilesInsideDir(app: AppData): List<File> {
        val dir = File(app.apkDir)
        return dir.walkTopDown().filter { apkDirFile ->
            apkDirFile.extension == APK_FILE_EXTENSION
        }.toList().also { apkFiles ->
            Log.d("FileUtil", "Got the apk list for ${app.name}: ${apkFiles.map { it.name }}")
        }
    }

    suspend fun getApkFileSizeSplitInfo(apkDirPath: String): Pair<Float, Boolean> = coroutineScope {
        val isSplit: Boolean
        File(apkDirPath).walkTopDown().filter { dirFile ->
            dirFile.isFile && dirFile.extension == APK_FILE_EXTENSION
        }.also { apkFiles ->
            isSplit = apkFiles.count() > 1
        }.sumOf { apkFile ->
            apkFile.length()
        }.toFloat() to isSplit
    }

    fun getApkZipFile(appBackupDirPath: String) =
        File(appBackupDirPath).walkTopDown().filter { backupFile ->
            backupFile.isFile && backupFile.extension == ZIP_FILE_EXTENSION
        }.map { fileWithZipExtension ->
            ZipFile(fileWithZipExtension)
        }.firstOrNull { zipFile ->
            zipFile.fileHeaders.map { fileHeader ->
                fileHeader.fileName.endsWith(".$APK_FILE_EXTENSION")
            }.all { fileNameHasApkExtension -> fileNameHasApkExtension }
        }
}