package com.stefan.simplebackup.ui.views

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.R
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class MainRecyclerView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : RecyclerView(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.recyclerViewStyle
    )

    fun canScrollUp() = canScrollVertically(-1)

    fun canScrollDown() = canScrollVertically(1)

    fun smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val scrollDuration = 380f
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return scrollDuration / computeVerticalScrollRange()
            }
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    inline fun onSaveRecyclerViewState(saveState: (Parcelable) -> Unit) {
        layoutManager?.onSaveInstanceState()?.let { stateParcelable ->
            saveState(stateParcelable)
        }
    }

    fun onRestoreRecyclerViewState(parcelable: Parcelable?) =
        parcelable?.let { stateParcelable ->
            layoutManager?.onRestoreInstanceState(stateParcelable)
        }

    override fun onCancelPendingInputEvents() {
        super.onCancelPendingInputEvents()
    }
}