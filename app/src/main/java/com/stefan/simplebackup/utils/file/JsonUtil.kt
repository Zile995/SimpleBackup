package com.stefan.simplebackup.utils.file

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
object JsonUtil {
    @Throws(IOException::class)
    suspend fun serializeApp(app: AppData, dir: String) {
        Log.d("Serialization", "Saving json to $dir/${app.name}.json")
        withContext(ioDispatcher) {
            try {
                Json.encodeToString(app).let { jsonString ->
                    val file = File(dir, app.name + ".json")
                    file.bufferedWriter().use { bufferedWriter ->
                        bufferedWriter.append(jsonString)
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is SerializationException -> e.localizedMessage?.let { message ->
                        Log.w("Serialization", message)
                    }
                    else -> throw e
                }
            }
        }
    }

    suspend fun deserializeApp(jsonFile: File): AppData? {
        Log.d("Serialization", "Creating the app from ${jsonFile.absolutePath}")
        val app: AppData
        return withContext(ioDispatcher) {
            try {
                jsonFile.inputStream().bufferedReader().use { reader ->
                    app = Json.decodeFromString(reader.readLine())
                }
                app
            } catch (e: Exception) {
                e.localizedMessage?.let { message ->
                    Log.w("Serialization", message)
                }
                null
            }
        }
    }
}