package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.MainApplication.Companion.backupDirPath
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
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
        File(path).listFiles()?.forEach { appDirList ->
            appDirList.listFiles()?.filter { appDirFile ->
                appDirFile.isFile && appDirFile.extension == "json"
            }?.map { jsonFile ->
                emit(jsonFile)
            }
        }
    }.flowOn(ioDispatcher)

    fun getApkZipFile(path: String): ZipFile? {
        val backupFiles = File(path)
        backupFiles.listFiles()?.filter { backupFile ->
            backupFile.isFile and (backupFile.extension == "zip")
        }?.forEach { file ->
            val zipFile = ZipFile(file)
            val headerList = zipFile.fileHeaders
            headerList.map { fileHeader ->
                if (fileHeader.fileName.endsWith("apk"))
                    return zipFile
            }
        }
        return null
    }
}