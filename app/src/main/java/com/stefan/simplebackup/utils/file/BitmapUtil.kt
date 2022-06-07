package com.stefan.simplebackup.utils.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object BitmapUtil {
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

            //Log.d("Bitmap", "Bytes bitmap: ${bitmap.allocationByteCount}")
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap
        }

    suspend fun Drawable.toByteArray(): ByteArray =
        this.toBitmap().toByteArray()

    suspend fun ByteArray.saveByteArray(
        fileName: String,
        context: Context
    ) {
        val byteArray = this
        withContext(ioDispatcher) {
            runCatching {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                    output.write(byteArray)
                    Log.d("Bitmap", "Saved bytearray")
                    output.close()
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("Serialization", message) }
            }
        }
    }

    suspend fun ByteArray.toBitmap(): Bitmap {
        val byteArray = this
        return withContext(ioDispatcher) {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }

    suspend fun Bitmap.toByteArray(): ByteArray =
        withContext(ioDispatcher) {
            val bytes = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.PNG, 100, bytes)
            bytes.toByteArray()
        }
}