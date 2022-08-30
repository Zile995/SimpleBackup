package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.view.doOnLayout
import androidx.core.view.postDelayed
import com.google.android.material.appbar.MaterialToolbar
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished

class SimpleMaterialToolbar(
    context: Context, attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialToolbar(context, attrs, defStyleAttr) {

    var inSearchState = false
    private val searchActionView
        get() =
            menu?.findItem(R.id.action_search)?.actionView as? MaterialSearchView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.toolbarStyle)

    fun removeTitle() {
        title = null
    }

    fun removeRipple() = setBackgroundResource(0)
    fun showKeyboard() = searchActionView?.requestFocus()
    fun hideKeyboard() = searchActionView?.resetSearchView()

    init {
        doOnLayout {
            setDefaultTitleTextColor()
        }
    }

    inline fun changeOnSearch(
        isSearching: Boolean,
        crossinline setNavigationOnClickListener: () -> Unit = {}
    ) {
        if (isSearching) {
            removeTitle()
            removeRipple()
            removeClickListeners()
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
            menu?.findItem(R.id.select_all)?.isVisible = false
            menu?.findItem(R.id.action_search)?.isVisible = true
            menu?.findItem(R.id.add_to_favorites)?.isVisible = false
            showKeyboard()
            setNavigationOnClickListener {
                if (animationFinished)
                    setNavigationOnClickListener.invoke()
            }
            inSearchState = false
        } else {
            postDelayed(50L) {
                setDefaultState()
            }
        }
    }

    inline fun changeOnSelection(
        isSelected: Boolean,
        crossinline selectionModeCallBack: SelectionModeCallBack = {}
    ) {
        if (isSelected) {
            removeRipple()
            removeClickListeners()
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.clear_selection)
            menu?.findItem(R.id.select_all)?.isVisible = true
            menu?.findItem(R.id.action_search)?.isVisible = false
            menu?.findItem(R.id.add_to_favorites)?.isVisible = true
            setNavigationOnClickListener {
                if (animationFinished)
                    selectionModeCallBack(false)
            }
            inSearchState = false
        } else {
            postDelayed(50L) {
                setDefaultState()
            }
        }
    }

    fun setDefaultState() {
        if (inSearchState) return
        hideKeyboard()
        addRipple()
        setDefaultTitle()
        propagateClickEventsToParent()
        setNavigationOnClickListener(null)
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
        inSearchState = true
    }

    private fun addRipple() =
        with(TypedValue()) {
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                this,
                true
            )
            setBackgroundResource(resourceId)
        }

    private fun setDefaultTitleTextColor() =
        with(TypedValue()) {
            context.theme.resolveAttribute(
                android.R.attr.textColorHint,
                this,
                true
            )
            setTitleTextColor(context.getColor(resourceId))
        }


    private fun setDefaultTitle() {
        title = context.getString(R.string.search_for_apps)
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
    }
}