package com.stefan.simplebackup.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.stefan.simplebackup.data.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import kotlin.math.pow

class FileUtil private constructor() {
    companion object {
        fun createDirectory(path: String) {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        fun createFile(path: String) {
            val file = File(path)
            file.createNewFile()
        }

        fun transformBytes(bytes: Float): String {
            return String.format("%3.1f %s", bytes / 1000.0.pow(2), "MB")
        }

        /**
         * - Prebacuje drawable u bitmap da bi je kasnije skladištili na internu memoriju
         */
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

        suspend fun appToJson(dir: String, app: Application) {
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
    }
}