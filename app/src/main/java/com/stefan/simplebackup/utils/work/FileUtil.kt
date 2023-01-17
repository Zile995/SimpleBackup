package com.stefan.simplebackup.utils.work

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.mainBackupDirPath
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.io.path.moveTo

const val LIB_DIR_NAME: String = "lib"
const val TEMP_DIR_NAME: String = "temp"
const val LOCAL_DIR_NAME: String = "local"
const val LIB_FILE_EXTENSION: String = "so"
const val APK_FILE_EXTENSION: String = "apk"
const val TAR_FILE_EXTENSION: String = "tar"
const val ZIP_FILE_EXTENSION: String = "zip"
const val JSON_FILE_EXTENSION: String = "json"

object FileUtil {
    // IO Dispatcher
    val ioDispatcher = Dispatchers.IO

    // Dir paths
    val tempDirPath = "${mainBackupDirPath}/$TEMP_DIR_NAME"
    val localDirPath = "${mainBackupDirPath}/$LOCAL_DIR_NAME"

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
    suspend inline fun deleteDirectoryFiles(
        dir: File,
        crossinline filter: (File) -> Boolean = { true }
    ) {
        if (!dir.isDirectory) throw IOException("File must be verified to be directory beforehand")
        withContext(ioDispatcher) {
            dir.walkTopDown().forEach { dirFile ->
                if (dirFile != dir && filter(dirFile))
                    deleteFile(dirFile.absolutePath)
            }
        }
    }

    @Throws(IOException::class)
    suspend fun deleteLocalBackup(packageName: String) = deleteFile("$localDirPath/$packageName")

    fun getTempDirPath(app: AppData): String = "$tempDirPath/${app.packageName}"

    fun getBackupDirPath(app: AppData): String = "$localDirPath/${app.packageName}"

    fun findTarArchive(dirPath: String, app: AppData): File {
        val tarArchive = File("$dirPath/${app.packageName}.$TAR_FILE_EXTENSION")
        if (!tarArchive.exists()) throw IOException("Unable to find tar archive")
        return tarArchive
    }

    suspend fun getJsonInDir(dirPath: String) = withContext(ioDispatcher) {
        File(dirPath).walkTopDown().firstOrNull { dirFile ->
            dirFile.isFile && dirFile.extension == JSON_FILE_EXTENSION
        }
    }

    suspend fun getJsonFileForApp(app: AppData): File? {
        val backupDirPath = getBackupDirPath(app)
        return getJsonInDir(dirPath = backupDirPath)
    }

    inline fun findJsonFiles(
        dirPath: String,
        crossinline filterDirNames: (String?) -> Boolean = { true }
    ) = flow {
        File(dirPath).walkTopDown().filter { dirFile ->
            dirFile.isFile
                    && filterDirNames(dirFile.parentFile?.name)
                    && dirFile.extension == JSON_FILE_EXTENSION
        }.forEach { jsonFile ->
            emit(jsonFile)
        }
    }.flowOn(ioDispatcher)

    fun getApkInDir(dirPath: String): List<File> {
        val dir = File(dirPath)
        return dir.walkTopDown().filter { dirFile ->
            dirFile.isFile && dirFile.extension == APK_FILE_EXTENSION
        }.toList()
    }

    suspend fun getApkSizeSplitInfo(dirPath: String): Pair<Float, Boolean> = coroutineScope {
        val isSplit: Boolean
        File(dirPath).walkTopDown().filter { dirFile ->
            dirFile.isFile && dirFile.extension == APK_FILE_EXTENSION
        }.also { apkFiles ->
            isSplit = apkFiles.count() > 1
        }.sumOf { apkFile ->
            apkFile.length()
        }.toFloat() to isSplit
    }
}