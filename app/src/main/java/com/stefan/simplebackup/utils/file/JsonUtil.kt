package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object JsonUtil {
    suspend fun serializeApp(app: AppData, dir: String) {
        Log.d("Serialization", "Saving json to $dir/${app.name}.json")
        withContext(ioDispatcher) {
            runCatching {
                Json.encodeToString(app).let { jsonString ->
                    val file = File(dir, app.name + ".json")
                    OutputStreamWriter(FileOutputStream(file)).use { outputStreamWriter ->
                        file.createNewFile()
                        outputStreamWriter.append(jsonString)
                    }
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("Serialization", message) }
            }
        }
    }

    suspend fun deserializeApp(jsonFile: File) = flow<AppData> {
        Log.d("Serialization", "Creating the app from ${jsonFile.absolutePath}")
        runCatching {
            jsonFile.inputStream()
                .bufferedReader()
                .use { reader ->
                    emit(Json.decodeFromString(reader.readLine()))
                }
        }.onFailure { throwable ->
            throwable.message?.let { message ->
                Log.e("Serialization", message)
            }
        }
    }
}