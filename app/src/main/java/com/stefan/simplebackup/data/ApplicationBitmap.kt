package com.stefan.simplebackup.data

import android.graphics.Bitmap

data class ApplicationBitmap(private val name: String,
                            private val icon: Bitmap) {

    fun getName() = this.name

    fun getIcon() = this.icon
}
