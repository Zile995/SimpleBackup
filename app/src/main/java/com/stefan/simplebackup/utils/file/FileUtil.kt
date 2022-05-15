package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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

    suspend fun findJsonFile(path: String) = flow {
        File(path).listFiles()?.filter { appDirFile ->
            appDirFile.isFile && appDirFile.extension == "json"
        }?.map { jsonFile ->
            JsonUtil.deserializeApp(jsonFile).collect {
                emit(it)
            }
        }
    }.flowOn(ioDispatcher)
}