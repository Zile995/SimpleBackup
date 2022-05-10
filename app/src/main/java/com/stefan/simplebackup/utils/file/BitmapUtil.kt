package com.stefan.simplebackup.utils.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object BitmapUtil {

    suspend fun appWithCheckedBitmap(app: AppData, context: Context): AppData {
        return if (app.bitmap.size > 200_000) {
            Log.d("Bitmap", "${app.bitmap.size}")
            saveBitmapByteArray(app.bitmap, app.name, context)
            app.copy(bitmap = byteArrayOf())
        } else {
            app
        }
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

    suspend fun setAppBitmap(app: AppData, context: Context) {
        val bitmapArray = app.bitmap
        withContext(ioDispatcher) {
            runCatching {
                if (bitmapArray.isEmpty()) {
                    val savedBitmapArray = context.openFileInput(app.name).readBytes()
                    app.bitmap = savedBitmapArray
                }
            }.also {
                if (bitmapArray.isEmpty()) {
                    context.deleteFile(app.name)
                }
            }.onFailure {
                it.message?.let { message -> Log.e("AppDetailActivity", message) }
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
}