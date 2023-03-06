package com.stefan.simplebackup.ui.views

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.inSelection
import kotlin.math.absoluteValue

class MainRecyclerView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : RecyclerView(context, attrs, defStyleAttr) {

    private var shouldInterceptScrolling = false
    private var areAllItemsVisible = false
    private val linearLayoutManager get() = layoutManager as LinearLayoutManager

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.recyclerViewStyle
    )

    private val recyclerViewSaver = object : OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            return if (shouldInterceptScrolling)
                e.actionMasked == MotionEvent.ACTION_MOVE
            else false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }

    init {
        isMotionEventSplittingEnabled = false
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            areAllItemsVisible = !canScrollUp() && !canScrollDown()
        }
        addOnItemTouchListener(recyclerViewSaver)
    }

    fun setInterceptScrolling(shouldIntercept: Boolean) {
        this.shouldInterceptScrolling = shouldIntercept
    }

    fun canScrollUp() = canScrollVertically(-1)
    fun canScrollDown() = canScrollVertically(1)

    fun shouldMoveAtLastCompletelyVisibleItem(): Boolean {
        val isLastItemVisible = isLastItemCompletelyVisible()
        return !areAllItemsVisible && isLastItemVisible
    }

    fun controlAttachedButton(floatingButton: ExtendedFloatingActionButton) =
        addOnScrollListener(object : OnScrollListener() {
            private var sensitivity = 18
            private var checkOnAttach = true

            private val buttonAction = object {
                fun onShow() = floatingButton.run {
                    if (inSelection) extend() else show()
                }

                fun onHide() = floatingButton.run {
                    if (inSelection) shrink() else hide()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Hide on attach only if first or last position is completely visible
                if (checkOnAttach) {
                    val firstItemPosition =
                        linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                    val lastItemPosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition()

                    if ((firstItemPosition == 0 || lastItemPosition == linearLayoutManager.itemCount - 1)
                        && floatingButton.isShown
                    ) {
                        buttonAction.onHide()
                    }
                    checkOnAttach = false
                }

                if (dy.absoluteValue < sensitivity) return
                if (dy > 0 && floatingButton.isShown) buttonAction.onHide()
                if (dy < 0) buttonAction.onShow()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (inSelection) return
                if (!canScrollUp() || !canScrollDown()) floatingButton.hide()
            }
        })

    fun slowlyScrollToLastItem() {
        if (linearLayoutManager.itemCount == 0) return
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return 1.75f
            }
        }
        smoothScroller.targetPosition = linearLayoutManager.itemCount - 1
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    fun smoothSnapToPosition(
        position: Int,
        scrollDuration: Float = 380f,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val scrollSpeed = scrollDuration / computeVerticalScrollRange()
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return scrollSpeed
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

    fun restoreRecyclerViewState(savedState: Parcelable?) =
        savedState?.let { stateParcelable ->
            layoutManager?.onRestoreInstanceState(stateParcelable)
        }

    private fun isLastItemCompletelyVisible(): Boolean {
        val lastItemPosition =
            linearLayoutManager.findLastCompletelyVisibleItemPosition()
        return lastItemPosition == linearLayoutManager.itemCount - 1
    }
}