package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

@Suppress("BlockingMethodInNonBlockingContext")
object JsonUtil {
    suspend fun serializeApp(app: AppData, dir: String) {
        Log.d("Serialization", "Saving json to $dir/${app.name}.json")
        withContext(ioDispatcher) {
            try {
                Json.encodeToString(app).let { jsonString ->
                    val file = File(dir, app.name + ".json")
                    OutputStreamWriter(FileOutputStream(file)).use { outputStreamWriter ->
                        file.createNewFile()
                        outputStreamWriter.append(jsonString)
                    }
                }
            } catch (e: SerializationException) {
                e.localizedMessage?.let { message ->
                    Log.d("Serialization", message)
                }
            }
        }
    }

    suspend fun deserializeApp(jsonFile: File) = flow<AppData> {
        Log.d("Serialization", "Creating the app from ${jsonFile.absolutePath}")
        try {
            jsonFile.inputStream()
                .bufferedReader()
                .use { reader ->
                    emit(Json.decodeFromString(reader.readLine()))
                }
        } catch (e: SerializationException) {
            e.localizedMessage?.let { message ->
                Log.d("Serialization", message)
            }
        }
    }
}