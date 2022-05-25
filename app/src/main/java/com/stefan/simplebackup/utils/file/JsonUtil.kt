package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.ioDispatcher
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

    fun deserializeApp(jsonFile: File): AppData? {
        Log.d("Serialization", "Creating the app from ${jsonFile.absolutePath}")
        val app: AppData
        return try {
            jsonFile.inputStream()
                .bufferedReader()
                .use { reader ->
                    app = Json.decodeFromString(reader.readLine())
                }
            app
        } catch (e: SerializationException) {
            e.localizedMessage?.let { message ->
                Log.d("Serialization", message)
            }
            null
        }
    }
}