package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

object FileUtil {
    suspend fun createDirectory(path: String) {
        withContext(ioDispatcher) {
            runCatching {
                val dir = File(path)
                if (!dir.exists()) {
                    Log.d("BaseUtil", "Creating the $path dir")
                    dir.mkdirs()
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("BaseUtil", "$path: $message") }
            }
        }
    }

    suspend fun createFile(path: String) {
        withContext(ioDispatcher) {
            runCatching {
                val file = File(path)
                file.createNewFile()
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("BaseUtil", "$path: $message") }
            }
        }
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