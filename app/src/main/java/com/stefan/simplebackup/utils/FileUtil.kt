package com.stefan.simplebackup.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.File
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
        fun drawableToBitmap(drawable: Drawable): Bitmap {
            val bitmap: Bitmap

            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            } else {
                bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}