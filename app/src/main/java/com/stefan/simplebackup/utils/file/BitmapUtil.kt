package com.stefan.simplebackup.utils.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
object BitmapUtil {

    private val ioDispatcher = Dispatchers.IO

    suspend fun Drawable.toBitmap(): Bitmap =
        withContext(ioDispatcher) {
            val bitmap: Bitmap =
                if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                    Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
                } else {
                    Bitmap.createBitmap(
                        intrinsicWidth,
                        intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                }
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap
        }

    suspend fun Drawable.toByteArray(): ByteArray =
        try {
            toBitmap().toByteArray()
        } catch (e: IOException) {
            byteArrayOf()
        }

    suspend fun ByteArray.saveByteArray(
        fileName: String,
        context: Context
    ) {
        val byteArray = this
        withContext(ioDispatcher) {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(byteArray)
                Log.d("Bitmap", "Saved bytearray")
                output.close()
            }
        }
    }

    @Throws(IOException::class)
    suspend fun ByteArray.toBitmap(): Bitmap {
        val byteArray = this
        return withContext(ioDispatcher) {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }

    @Throws(IOException::class)
    private suspend fun Bitmap.toByteArray(): ByteArray =
        withContext(ioDispatcher) {
            val bytes = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.PNG, 100, bytes)
            bytes.toByteArray()
        }
}