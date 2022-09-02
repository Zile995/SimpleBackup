package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
import com.google.android.material.appbar.MaterialToolbar
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished

class SimpleMaterialToolbar(
    context: Context, attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialToolbar(context, attrs, defStyleAttr) {

    private val deleteItem
        get() = findMenuItem(R.id.delete)

    private val addToFavoritesItem
        get() = findMenuItem(R.id.add_to_favorites)

    private val selectAllItem
        get() = findMenuItem(R.id.select_all)

    private val searchViewItem
        get() = findMenuItem(R.id.action_search)

    private val searchActionView
        get() =
            searchViewItem?.actionView as? MaterialSearchView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.toolbarStyle)

    init {
        setDefaultTitleTextColor()
    }

    fun changeOnSearch(
        isSearching: Boolean,
        setNavigationOnClickListener: () -> Unit = {}
    ) {
        inSearchState = isSearching
        if (isSearching) {
            removeTitle()
            removeRipple()
            removeOnClickListener()
            setMenuItemsOnSearch()
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
            setNavigationOnClickListener {
                if (animationFinished)
                    setNavigationOnClickListener()
            }
        } else {
            postDelayed(50L) {
                setDefaultState()
            }
        }
    }

    fun changeOnSelection(
        isSelected: Boolean,
        selectionModeCallBack: SelectionModeCallBack = {}
    ) {
        inSelectionState = isSelected
        if (isSelected) {
            removeRipple()
            removeOnClickListener()
            setMenuItemsOnSelection()
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.clear_selection)
            setNavigationOnClickListener {
                if (animationFinished)
                    selectionModeCallBack(false)
            }
        } else {
            postDelayed(50L) {
                setDefaultState()
            }
        }
    }

    fun changeOnSettings(
        isInSettings: Boolean,
        setNavigationOnClickListener: () -> Unit = {}
    ) {
        inSettingsState = isInSettings
        if (isInSettings) {
            removeRipple()
            setDefaultMenuItems()
            removeOnClickListener()
            setCustomTitle(R.string.settings)
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
            setNavigationOnClickListener {
                if (animationFinished) {
                    setNavigationOnClickListener()
                }
            }
        } else {
            postDelayed(50L) {
                setDefaultState()
            }
        }
    }

    private fun setDefaultState() {
        if (!(inSearchState || inSelectionState || inSettingsState)) {
            Log.d("SimpleMaterialToolbar", "Setting to default state")
            addRipple()
            setDefaultTitle()
            setDefaultMenuItems()
            propagateClickEventsToParent()
            setNavigationIcon(R.drawable.ic_search)
            setNavigationContentDescription(R.string.search_for_apps)
            setNavigationOnClickListener {
                if (animationFinished) {
                    (parent as View).performClick()
                }
            }
        }
    }

    fun changeOnFavorite(isFavorite: Boolean) {
        when {
            isFavorite -> {
                deleteItem?.isVisible = false
                addToFavoritesItem?.icon = getDrawable(R.drawable.ic_remove_favorite)
                addToFavoritesItem?.tooltipText = context.getString(R.string.remove_from_favorites)
            }
            else -> {
                addToFavoritesItem?.icon = getDrawable(R.drawable.ic_favorite)
                addToFavoritesItem?.tooltipText = context.getString(R.string.add_to_favorites)
            }
        }
    }

    private fun setMenuItemsOnSearch() {
        deleteItem?.isVisible = false
        searchViewItem?.isVisible = true
        selectAllItem?.isVisible = false
        addToFavoritesItem?.isVisible = false
        searchActionView?.requestFocus()
    }

    private fun setMenuItemsOnSelection() {
        deleteItem?.isVisible = true
        selectAllItem?.isVisible = true
        searchViewItem?.isVisible = false
        addToFavoritesItem?.isVisible = true
    }

    private fun setDefaultMenuItems() {
        resetSearchActionView()
        deleteItem?.isVisible = false
        selectAllItem?.isVisible = false
        searchViewItem?.isVisible = false
        addToFavoritesItem?.isVisible = false
    }

    private fun removeTitle() = run { title = null }
    private fun removeRipple() = setBackgroundResource(0)
    private fun removeOnClickListener() = setOnClickListener(null)
    private fun setCustomTitle(@StringRes customTitle: Int) =
        run { title = context.getString(customTitle) }

    private fun getDrawable(@DrawableRes resourceId: Int) =
        ContextCompat.getDrawable(context, resourceId)

    private fun resetSearchActionView() = searchActionView?.resetSearchView()
    private fun findMenuItem(@IdRes resourceId: Int) = menu?.findItem(resourceId)

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


    private fun propagateClickEventsToParent() {
        val parentView by lazy { parent as View }
        setOnClickListener {
            if (animationFinished) {
                parentView.performClick()
            }
        }
    }

    private companion object {
        var inSearchState = false
        var inSettingsState = false
        var inSelectionState = false
    }
}