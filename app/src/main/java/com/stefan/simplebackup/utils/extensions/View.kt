package com.stefan.simplebackup.utils.extensions

import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import com.stefan.simplebackup.ui.activities.FloatingButtonCallback

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
    val imageLoader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(byteArray)
        .crossfade(true)
        .target(this)
        .build()
    imageLoader.enqueue(request)
}

fun RecyclerView.onSaveRecyclerViewState(saveState: (Parcelable) -> Unit) {
    layoutManager?.onSaveInstanceState()?.let { stateParcelable ->
        saveState(stateParcelable)
    }
}

fun RecyclerView.onRestoreRecyclerViewState(parcelable: Parcelable?) {
    parcelable?.let { stateParcelable ->
        layoutManager?.onRestoreInstanceState(stateParcelable)
    }
}

fun RecyclerView.canScrollUp() = canScrollVertically(-1)

fun RecyclerView.canScrollDown() = canScrollVertically(1)

fun RecyclerView.hideAttachedButton(
    isButtonVisible: Boolean,
    shouldShowButton: FloatingButtonCallback
) {
    val linearLayoutManager = layoutManager as LinearLayoutManager
    var checkOnAttach = true
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (checkOnAttach) {
                val firstItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                val lastItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                if (firstItemPosition == 0) {
                    shouldShowButton(false)
                } else {
                    shouldShowButton(true)
                }
                if (lastItemPosition == linearLayoutManager.itemCount - 1) {
                    shouldShowButton(false)
                }
                checkOnAttach = false
            }

            if (dy > 0) {
                shouldShowButton(false)
            } else if (dy < 0) {
                shouldShowButton(true)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (!canScrollUp() || !canScrollDown()) shouldShowButton(false)
        }
    })
}