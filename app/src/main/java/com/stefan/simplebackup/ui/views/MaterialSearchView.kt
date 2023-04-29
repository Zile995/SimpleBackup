package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.marginLeft
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.showSoftKeyboard

class MaterialSearchView(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int
) : SearchView(context, attrs, defStyleAttr) {

    private val searchText: TextView =
        findViewById(com.google.android.material.R.id.search_src_text)
    private val searchEditFrame: View =
        findViewById(com.google.android.material.R.id.search_edit_frame)
    private val closeButton: ImageView? =
        findViewById(com.google.android.material.R.id.search_close_btn)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context, attrs, androidx.appcompat.R.attr.searchViewStyle
    )

    init {
        addCloseButton()
        setSearchViewMargin()
        setSearchTextAppearance()
        preventFullScreenKeyboard()
        setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus)
                view.showSoftKeyboard()
        }
    }

    private fun addCloseButton() = closeButton?.run {
        val layoutParams: LinearLayout.LayoutParams =
            this.layoutParams as LinearLayout.LayoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        setImageResource(R.drawable.ic_close)
    }

    private fun preventFullScreenKeyboard() {
        searchText.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_SEARCH
    }

    private fun setSearchViewMargin() {
        maxWidth = Integer.MAX_VALUE
        val params = searchEditFrame.layoutParams as LinearLayout.LayoutParams
        val leftMargin = -searchEditFrame.marginLeft
        params.setMargins(leftMargin, 0, 30, 0)
        searchEditFrame.layoutParams = params
    }

    private fun setSearchTextAppearance() =
        searchText.setTextAppearance(R.style.TextAppearance_SimpleBackup_TitleSmall)

    fun clearSearchViewText() = setQuery("", false)
}