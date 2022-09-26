package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.backupDirPath
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

const val BACKUP_DIR_PATH: String = "SimpleBackup/local"
const val TEMP_DIR_PATH: String = "SimpleBackup/temp"

@Suppress("BlockingMethodInNonBlockingContext")
object FileUtil {

    private val ioDispatcher = Dispatchers.IO

    @Throws(IOException::class)
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
    suspend fun createFile(path: String) {
        withContext(ioDispatcher) {
            val file = File(path)
            file.createNewFile()
        }
    }

    @Throws(IOException::class)
    suspend fun deleteFile(path: String) {
        withContext(ioDispatcher) {
            Log.d("FileUtil", "Deleting the $path")
            val file = File(path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }

    @Throws(IOException::class)
    // TODO: Fix file moving, as backup solutions...
    suspend fun File.moveFile(targetFile: File) {
        val sourceFilePath = this.toPath()
        withContext(ioDispatcher) {
            targetFile.delete()
            sourceFilePath.moveTo(targetFile.toPath(), true)
        }
    }

    suspend fun createMainDir() {
        FileUtil.apply {
            createDirectory(backupDirPath)
            createFile("${backupDirPath}/.nomedia")
        }
    }

    fun getBackupDirPath(app: AppData) = "${backupDirPath}/${app.packageName}"

    fun getTempDirPath(app: AppData): String {
        val tempDirPath = backupDirPath.replace(BACKUP_DIR_PATH, TEMP_DIR_PATH)
        return "$tempDirPath/${app.packageName}"
    }

    suspend fun moveBackup(app: AppData) {
        val sourceFile = File(getTempDirPath(app))
        val targetFile = File(getBackupDirPath(app) + "/")
        sourceFile.moveFile(targetFile)
    }

    suspend fun findJsonFiles(path: String) = flow {
        File(path).walkTopDown().filter {
            it.isFile && it.extension == "json"
        }.forEach { jsonFile ->
            emit(jsonFile)
        }
    }.flowOn(ioDispatcher)

    fun getApkFilesInsideDir(app: AppData): MutableList<File> {
        val apkFiles = mutableListOf<File>()
        val dir = File(app.apkDir)
        dir.walkTopDown().filter {
            it.extension == "apk"
        }.forEach { apkFile ->
            apkFiles.add(apkFile)
        }
        Log.d("FileUtil", "Got the apk list for ${app.name}: ${apkFiles.map { it.name }}")
        return apkFiles
    }

    suspend fun getApkFileSizeSplitInfo(apkDirPath: String): Pair<Float, Boolean> = coroutineScope {
        val isSplit: Boolean
        File(apkDirPath).listFiles()?.let { apkDirFiles ->
            apkDirFiles.filter { dirFile ->
                dirFile.isFile && dirFile.name.endsWith(".apk")
            }.also { apkFiles ->
                isSplit = apkFiles.size > 1
            }.sumOf { apkFile ->
                apkFile.length()
            }.toFloat() to isSplit
        } ?: (0f to false)
    }

    fun getApkZipFile(apkZipDirPath: String): ZipFile? {
        val backupFiles = File(apkZipDirPath)
        backupFiles.listFiles()?.filter { backupFile ->
            backupFile.isFile and (backupFile.extension == "zip")
        }?.forEach { file ->
            val zipFile = ZipFile(file)
            val headerList = zipFile.fileHeaders
            headerList.map { fileHeader ->
                fileHeader.fileName.endsWith("apk")
            }.all { it }
        }
        return null
    }
}