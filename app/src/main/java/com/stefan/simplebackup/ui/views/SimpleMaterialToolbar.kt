package com.stefan.simplebackup.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.view.doOnPreDraw
import com.google.android.material.appbar.MaterialToolbar
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished
import com.stefan.simplebackup.utils.extensions.getAttributeResourceId
import com.stefan.simplebackup.utils.extensions.getResourceDrawable

class SimpleMaterialToolbar(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int
) : MaterialToolbar(context, attrs, defStyleAttr) {

    private var hasOnTouchListener = false

    val deleteItem
        get() = findMenuItem(R.id.delete)

    val searchActionView
        get() = searchViewItem?.actionView as? MaterialSearchView

    val addToFavoritesItem
        get() = findMenuItem(R.id.add_to_favorites)

    private val selectAllItem
        get() = findMenuItem(R.id.select_all)

    private val searchViewItem
        get() = findMenuItem(R.id.action_search)

    private val parentView get() = parent as? View

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, androidx.appcompat.R.attr.toolbarStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        addRipple()
    }

    inline fun changeOnSearch(
        isSearching: Boolean, crossinline onNavigationClickAction: () -> Unit = {}
    ) {
        if (isSearching) {
            post {
                removeTitle()
                setMenuItemsOnSearch()
                removeOnTouchListener()
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
        isSelected: Boolean, crossinline selectionModeCallBack: SelectionModeCallBack = {}
    ) {
        if (isSelected) {
            removeOnTouchListener()
            setMenuItemsOnSelection()
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.clear_selection)
            setNavigationOnClickListener {
                if (animationFinished) selectionModeCallBack(false)
            }
        } else setDefaultState()
    }

    inline fun changeOnSettings(
        isInSettings: Boolean, crossinline onNavigationClickAction: () -> Unit = {}
    ) {
        if (isInSettings) {
            post {
                setDefaultMenuItems()
                removeOnTouchListener()
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
        post {
            if (!hasOnTouchListener) {
                Log.d("SimpleMaterialToolbar", "Setting to default state")
                setDefaultTitle()
                setDefaultMenuItems()
                setDefaultListeners()
                setNavigationIcon(R.drawable.ic_search)
                setNavigationContentDescription(R.string.search_for_apps)
            }
        }
    }

    fun changeOnFavorite(isFavorite: Boolean) {
        when {
            isFavorite -> {
                deleteItem?.isVisible = false
                addToFavoritesItem?.icon =
                    context.getResourceDrawable(R.drawable.ic_remove_favorite)
                addToFavoritesItem?.tooltipText = context.getString(R.string.remove_from_favorites)
            }
            else -> {
                deleteItem?.isVisible = true
                addToFavoritesItem?.icon = context.getResourceDrawable(R.drawable.ic_favorite)
                addToFavoritesItem?.tooltipText = context.getString(R.string.add_to_favorites)
            }
        }
    }

    fun setCustomTitle(
        titleText: String?, @StyleRes resId: Int = R.style.TextAppearance_SimpleBackup_TitleSmall
    ) {
        setTitleTextAppearance(context, resId)
        title = titleText
    }

    fun setCustomTitle(
        @StringRes customTitle: Int, @StyleRes styleResId: Int
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
        deleteItem?.isVisible = true
        selectAllItem?.isVisible = true
        searchViewItem?.isVisible = false
        addToFavoritesItem?.isVisible = true
    }

    fun setDefaultMenuItems() {
        deleteItem?.isVisible = false
        selectAllItem?.isVisible = false
        addToFavoritesItem?.isVisible = false
        searchActionView?.clearSearchViewText()
    }

    fun removeTitle() = run { title = null }

    fun removeOnTouchListener() {
        setOnTouchListener(null)
        hasOnTouchListener = false
    }

    fun resetSearchActionView() {
        searchActionView?.clearFocus()
        searchViewItem?.isVisible = false
    }

    fun requestSearchActionViewFocus() = searchActionView?.requestFocus()

    private fun findMenuItem(@IdRes resourceId: Int) = menu?.findItem(resourceId)

    private fun setDefaultTitleTextColor() = with(TypedValue()) {
        context.theme.resolveAttribute(
            android.R.attr.textColorHint, this, true
        )
        setTitleTextColor(context.getColor(resourceId))
    }

    private fun addRipple() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val resourceId =
                getAttributeResourceId(android.R.attr.selectableItemBackgroundBorderless)
            setBackgroundResource(resourceId)
        }
    }

    private fun setDefaultTitle() {
        setCustomTitle(R.string.search_for_apps, R.style.TextAppearance_SimpleBackup_TitleSmall)
        setDefaultTitleTextColor()
    }

    private inline fun performParentClick(onTouch: () -> Boolean?) =
        if (animationFinished) {
            onTouch() ?: false
        } else false

    @SuppressLint("ClickableViewAccessibility")
    private fun setDefaultListeners() {
        setOnTouchListener { _, event -> performParentClick { parentView?.onTouchEvent(event) } }
        setNavigationOnClickListener { performParentClick { parentView?.callOnClick() } }
        hasOnTouchListener = true
    }
}