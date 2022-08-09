package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.google.android.material.appbar.MaterialToolbar
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished


class SimpleMaterialToolbar(
    context: Context, attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialToolbar(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.toolbarStyle)

    init {
        propagateClickEventsToParent()
    }

    inline fun changeWhenSearching(
        isSearching: Boolean,
        crossinline setNavigationOnClickListener: () -> Unit = {}
    ) {
        if (isSearching) {
            removeTitle()
            removeRipple()
            removeClickListeners()
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
            menu?.findItem(R.id.add_to_favorites)?.isVisible = false
            menu?.findItem(R.id.select_all)?.isVisible = false
            menu?.findItem(R.id.action_search)?.isVisible = true
            setNavigationOnClickListener {
                if (animationFinished) {
                    setNavigationOnClickListener.invoke()
                }
            }
        } else {
            resetToSearchState()
        }

    }

    inline fun changeOnSelection(
        isSelected: Boolean,
        crossinline selectionModeCallBack: SelectionModeCallBack = {}
    ) {
        if (isSelected) {
            removeTitle()
            removeRipple()
            removeClickListeners()
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.clear_selection)
            menu?.findItem(R.id.select_all)?.isVisible = true
            menu?.findItem(R.id.add_to_favorites)?.isVisible = true
            menu?.findItem(R.id.action_search)?.isVisible = false
            setNavigationOnClickListener {
                if (animationFinished) {
                    selectionModeCallBack.invoke(false)
                }
            }
        } else {
            resetToSearchState()
        }
    }

    fun resetToSearchState() {
        addRipple()
        setDefaultTitle()
        propagateClickEventsToParent()
        setNavigationIcon(R.drawable.ic_search)
        setNavigationContentDescription(R.string.search_for_apps)
        menu?.findItem(R.id.select_all)?.isVisible = false
        menu?.findItem(R.id.action_search)?.isVisible = false
        menu?.findItem(R.id.add_to_favorites)?.isVisible = false
        setNavigationOnClickListener {
            if (animationFinished) {
                (parent as View).performClick()
            }
        }
    }

    private fun addRipple() {
        with(TypedValue()) {
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                this,
                true
            )
            setBackgroundResource(resourceId)
        }
    }

    fun removeRipple() {
        setBackgroundResource(0)
    }

    fun removeTitle() {
        title = null
    }

    private fun setDefaultTitle() {
        title = context.getString(R.string.search_for_apps)
        with(TypedValue()) {
            context.theme.resolveAttribute(
                android.R.attr.textColorHint,
                this,
                true
            )
            setTitleTextColor(context.getColor(resourceId))
        }
    }

    fun removeClickListeners() {
        setOnClickListener(null)
        setOnLongClickListener(null)
    }

    private fun propagateClickEventsToParent() {
        val parentView by lazy { parent as View }
        setOnClickListener {
            if (animationFinished) {
                parentView.performClick()
            }
        }
        setOnLongClickListener {
            parentView.performLongClick()
        }
    }
}