package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
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

    inline fun changeWhenSearching(
        isSearching: Boolean,
        crossinline setNavigationOnClickListener: () -> Unit = {}
    ) {
        if (isSearching) {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
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
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.clear_selection)
            menu?.findItem(R.id.select_all)?.isVisible = true
            menu?.findItem(R.id.action_search)?.isVisible = false
            setNavigationOnClickListener {
                if (animationFinished) {
                    selectionModeCallBack.invoke(!isSelected)
                }
            }
        } else {
            menu?.findItem(R.id.select_all)?.isVisible = false
            resetToSearchState()
        }
    }

    fun resetToSearchState() {
        setNavigationIcon(R.drawable.ic_search)
        setNavigationContentDescription(R.string.search_for_apps)
        menu?.findItem(R.id.action_search)?.isVisible = true
        setNavigationOnClickListener {
            if (animationFinished) {
                (parent as View).performClick()
            }
        }
    }
}