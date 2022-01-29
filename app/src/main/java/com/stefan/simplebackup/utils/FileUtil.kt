package com.stefan.simplebackup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlin.math.pow

object FileUtil {

    fun createDirectory(path: String) {
        runCatching {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }.onFailure { throwable ->
            throwable.message?.let { message -> Log.e("FileUtil", "$path: $message") }
        }
    }

    fun createFile(path: String) {
        runCatching {
            val file = File(path)
            file.createNewFile()
        }.onFailure { throwable ->
            throwable.message?.let { message -> Log.e("FileUtil", "$path: $message") }
        }
    }

    fun transformBytes(bytes: Float): String {
        return String.format("%3.1f %s", bytes / 1000.0.pow(2), "MB")
    }

    fun drawableToByteArray(drawable: Drawable): ByteArray {
        val bitmap: Bitmap =
            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            } else {
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            }

        Log.d("Bitmap", "Bytes bitmap: ${bitmap.allocationByteCount}")
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmapToByteArray(bitmap)
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        return bytes.toByteArray()
    }

    fun saveBitmap(bitmap: Bitmap, fileName: String, context: Context) {
        try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(bytes.toByteArray())
                output.close()
            }
        } catch (e: Throwable) {
            Log.e("Bitmap", "Bitmap error + ${e.message}")
        }
    }

    suspend fun appToJson(dir: String, app: AppData) {
        withContext(Dispatchers.Default) {
            runCatching {
                val file = File(dir, app.getName() + ".json")
                OutputStreamWriter(FileOutputStream(file)).use { outputStreamWriter ->
                    file.createNewFile()
                    outputStreamWriter.append(Json.encodeToString(app))
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("Serialization", message) }
            }
        }
    }

    suspend fun jsonToApp(jsonFile: File) = flow<AppData> {
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