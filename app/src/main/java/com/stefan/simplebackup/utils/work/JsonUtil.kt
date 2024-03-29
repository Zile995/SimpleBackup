package com.stefan.simplebackup.utils.work

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

object JsonUtil {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(IOException::class)
    suspend fun serializeApp(app: AppData, destinationPath: String) {
        Log.d("Serialization", "Saving json to $destinationPath/${app.name}.$JSON_FILE_EXTENSION")
        withContext(ioDispatcher) {
            try {
                Json.encodeToString(app).let { jsonString ->
                    val file = File(destinationPath, app.name + ".$JSON_FILE_EXTENSION")
                    file.bufferedWriter().use { bufferedWriter ->
                        bufferedWriter.append(jsonString)
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is IllegalArgumentException -> {
                        Log.w(
                            "Serialization",
                            "Error occurred $e ${e.message}"
                        ); throw IOException()
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
                Log.w("Serialization", "Error occurred $e ${e.message}")
                null
            }
        }
    }
}