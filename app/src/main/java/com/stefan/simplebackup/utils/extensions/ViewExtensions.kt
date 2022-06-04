package com.stefan.simplebackup.utils.extensions

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.GlideApp

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        when {
            value -> show()
            else -> hide()
        }
    }

fun ImageView.loadBitmap(byteArray: ByteArray) {
    val image = this
    GlideApp.with(image).apply {
        asBitmap()
            .load(byteArray)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(image)
    }
}

fun RecyclerView.hideAttachedButton(floatingButton: FloatingActionButton) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy > 0 && floatingButton.isShown) {
                floatingButton.hide()

            } else if (dy < 0 && !floatingButton.isShown) {
                floatingButton.show()
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (!recyclerView.canScrollVertically(-1)) {
                floatingButton.hide()
            }
        }
    })
}