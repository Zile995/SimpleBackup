package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import com.google.android.material.appbar.MaterialToolbar
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished

class SimpleMaterialToolbar(
    context: Context, attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialToolbar(context, attrs, defStyleAttr) {

    val deleteItem
        get() = findMenuItem(R.id.delete)

    val searchActionView
        get() =
            searchViewItem?.actionView as? MaterialSearchView

    val addToFavoritesItem
        get() = findMenuItem(R.id.add_to_favorites)

    private val selectAllItem
        get() = findMenuItem(R.id.select_all)

    private val searchViewItem
        get() = findMenuItem(R.id.action_search)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.toolbarStyle)

    init {
        contentInsetStartWithNavigation = 0
    }

    inline fun changeOnSearch(
        isSearching: Boolean,
        crossinline onNavigationClickAction: () -> Unit = {}
    ) {
        if (isSearching) {
            post {
                removeTitle()
                removeRipple()
                setMenuItemsOnSearch()
                removeOnClickListener()
                setNavigationIcon(R.drawable.ic_arrow_back)
                setNavigationContentDescription(R.string.back)
                setNavigationOnClickListener {
                    if (animationFinished) {
                        doOnPreDraw {
                            onNavigationClickAction()
                        }
                    }
                }
            }
        } else setDefaultState()
    }

    inline fun changeOnSelection(
        isSelected: Boolean,
        crossinline selectionModeCallBack: SelectionModeCallBack = {}
    ) {
        if (isSelected) {
            post {
                removeRipple()
                removeOnClickListener()
                setMenuItemsOnSelection()
                setNavigationIcon(R.drawable.ic_close)
                setNavigationContentDescription(R.string.clear_selection)
                setNavigationOnClickListener {
                    if (animationFinished)
                        doOnPreDraw {
                            selectionModeCallBack(false)
                        }
                }
            }
        } else setDefaultState()
    }

    inline fun changeOnSettings(
        isInSettings: Boolean,
        crossinline onNavigationClickAction: () -> Unit = {}
    ) {
        if (isInSettings) {
            post {
                removeRipple()
                setDefaultMenuItems()
                removeOnClickListener()
                setCustomTitle(R.string.settings, R.style.TextAppearance_SimpleBackup_TitleMedium)
                setNavigationIcon(R.drawable.ic_arrow_back)
                setNavigationContentDescription(R.string.back)
                setNavigationOnClickListener {
                    if (animationFinished) {
                        doOnPreDraw {
                            onNavigationClickAction()
                        }
                    }
                }
            }
        } else setDefaultState()
    }

    fun setDefaultState() {
        if (!hasOnClickListeners()) {
            post {
                Log.d("SimpleMaterialToolbar", "Setting to default state")
                addRipple()
                setDefaultTitle()
                setDefaultMenuItems()
                propagateClickEventsToParent()
                setNavigationIcon(R.drawable.ic_search)
                setNavigationContentDescription(R.string.search_for_apps)
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
                deleteItem?.isVisible = true
                addToFavoritesItem?.icon = getDrawable(R.drawable.ic_favorite)
                addToFavoritesItem?.tooltipText = context.getString(R.string.add_to_favorites)
            }
        }
    }

    fun setCustomTitle(
        titleText: String?,
        @StyleRes resId: Int = R.style.TextAppearance_SimpleBackup_TitleSmall
    ) {
        setTitleTextAppearance(context, resId)
        title = titleText
    }

    fun setCustomTitle(
        @StringRes customTitle: Int,
        @StyleRes styleResId: Int
    ) {
        setTitleTextAppearance(context, styleResId)
        title = context.getString(customTitle)
    }

    fun setMenuItemsOnSearch() {
        searchViewItem?.isVisible = true
        addToFavoritesItem?.isVisible = false
        deleteItem?.isVisible = false
        selectAllItem?.isVisible = false
    }

    fun setMenuItemsOnSelection() {
        searchViewItem?.isVisible = false
        addToFavoritesItem?.isVisible = true
        deleteItem?.isVisible = true
        selectAllItem?.isVisible = true
    }

    fun setDefaultMenuItems() {
        deleteItem?.isVisible = false
        selectAllItem?.isVisible = false
        addToFavoritesItem?.isVisible = false
    }

    fun removeTitle() = run { title = null }
    fun removeRipple() = setBackgroundResource(0)
    fun removeOnClickListener() {
        setOnClickListener(null)
        setNavigationOnClickListener(null)
    }

    fun resetSearchActionView() {
        searchActionView?.clearFocus()
        searchViewItem?.isVisible = false
    }

    fun requestSearchActionViewFocus() = searchActionView?.requestFocus()

    private fun getDrawable(@DrawableRes resourceId: Int) =
        ContextCompat.getDrawable(context, resourceId)

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
        setCustomTitle(R.string.search_for_apps, R.style.TextAppearance_SimpleBackup_TitleSmall)
        setDefaultTitleTextColor()
    }

    private fun propagateClickEventsToParent() {
        val parentView by lazy { parent as View }
        setOnClickListener {
            if (animationFinished) {
                searchActionView?.clearSearchViewText()
                parentView.callOnClick()
            }
        }
        setNavigationOnClickListener {
            if (animationFinished) {
                searchActionView?.clearSearchViewText()
                parentView.callOnClick()
            }
        }
    }
}