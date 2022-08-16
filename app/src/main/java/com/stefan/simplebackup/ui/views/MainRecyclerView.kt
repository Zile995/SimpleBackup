package com.stefan.simplebackup.ui.views

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.selectionFinished

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

    init {
        isMotionEventSplittingEnabled = false
    }

    fun canScrollUp() = canScrollVertically(-1)

    fun canScrollDown() = canScrollVertically(1)

    fun shouldMoveAtLastCompletelyVisibleItem(): Boolean {
        val isVisible = isLastItemCompletelyVisible()
        return if (isVisible) {
            adapter?.let { smoothSnapToPosition(it.itemCount) }
            true
        } else false
    }

    fun hideAttachedButton(floatingButton: MainFloatingButton) {
        var showAction: () -> Unit
        var hideAction: () -> Unit
        var checkOnAttach = true
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!selectionFinished) {
                    showAction = { floatingButton.extend() }
                    hideAction = { floatingButton.shrink() }
                } else {
                    showAction = { floatingButton.show() }
                    hideAction = { floatingButton.hide() }
                }

                if (checkOnAttach) {
                    val linearLayoutManager = layoutManager as LinearLayoutManager
                    val firstItemPosition =
                        linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                    val lastItemPosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition()

                    if ((firstItemPosition == 0 || lastItemPosition == linearLayoutManager.itemCount - 1)
                        && floatingButton.isShown
                    ) {
                        hideAction()
                    }
                    checkOnAttach = false
                }

                if (dy > 0) {
                    hideAction()
                } else if (dy < 0) {
                    showAction()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!selectionFinished) return
                if (!canScrollUp() || !canScrollDown()) floatingButton.hide()
            }
        })
    }

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

    fun restoreRecyclerViewState(savedState: Parcelable?) =
        savedState?.let { stateParcelable ->
            layoutManager?.onRestoreInstanceState(stateParcelable)
        }

    private fun isLastItemCompletelyVisible(): Boolean {
        val linearLayoutManager = layoutManager as LinearLayoutManager
        val lastItemPosition =
            linearLayoutManager.findLastCompletelyVisibleItemPosition()
        return lastItemPosition == linearLayoutManager.itemCount - 1
    }
}