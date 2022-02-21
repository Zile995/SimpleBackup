package com.stefan.simplebackup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.CoroutineDispatcher
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

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun createDirectory(path: String) {
        withContext(ioDispatcher) {
            runCatching {
                val dir = File(path)
                if (!dir.exists()) {
                    Log.d("FileUtil", "Creating the $path dir")
                    dir.mkdirs()
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("FileUtil", "$path: $message") }
            }
        }
    }

    suspend fun createFile(path: String) {
        withContext(ioDispatcher) {
            runCatching {
                val file = File(path)
                file.createNewFile()
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("FileUtil", "$path: $message") }
            }
        }
    }

    fun <T: Number> transformBytesToString(bytes: T): String {
        return String.format("%3.1f %s", bytes.toFloat() / 1000.0.pow(2), "MB")
    }

    suspend fun drawableToByteArray(drawable: Drawable): ByteArray =
        withContext(ioDispatcher) {
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
            bitmapToByteArray(bitmap)
        }

    private suspend fun bitmapToByteArray(bitmap: Bitmap): ByteArray =
        withContext(ioDispatcher) {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            bytes.toByteArray()
        }

    suspend fun saveBigBitmap(item: AppData, context: Context) {
        val bitmapByteArray = item.getBitmap()
        Log.d("Bitmap", "${bitmapByteArray.size}")
        if (bitmapByteArray.size > 200000) {
            saveBitmapByteArray(bitmapByteArray, item.getName(), context)
            item.setBitmap(byteArrayOf())
        }
    }

    suspend fun setAppBitmap(app: AppData, context: Context) {
        val bitmapArray = app.getBitmap()
        withContext(ioDispatcher) {
            runCatching {
                if (bitmapArray.isEmpty()) {
                    val savedBitmapArray = context.openFileInput(app.getName()).readBytes()
                    app.setBitmap(savedBitmapArray)
                }
            }.onSuccess {
                if (bitmapArray.isEmpty()) {
                    context.deleteFile(app.getName())
                }
            }.onFailure {
                it.message?.let { message -> Log.e("BackupActivity", message) }
            }
        }
    }

    private suspend fun saveBitmapByteArray(
        byteArray: ByteArray,
        fileName: String,
        context: Context
    ) {
        withContext(ioDispatcher) {
            runCatching {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                    output.write(byteArray)
                    output.close()
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("Serialization", message) }
            }
        }
    }

    suspend fun serializeApp(app: AppData, dir: String) {
        Log.d("Serialization", "Saving json to $dir/${app.getName()}.json")
        withContext(ioDispatcher) {
            runCatching {
                Json.encodeToString(app).let { jsonString ->
                    val file = File(dir, app.getName() + ".json")
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