package com.stefan.simplebackup.ui.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.getInterFontTypeFace
import com.stefan.simplebackup.utils.extensions.showSoftKeyboard

class MaterialSearchView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : SearchView(context, attrs, defStyleAttr) {

    private val searchText: TextView = findViewById(R.id.search_src_text)
    private val searchEditFrame: View = findViewById(R.id.search_edit_frame)
    private val closeButton: ImageView? = findViewById(R.id.search_close_btn)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.searchViewStyle
    )

    init {
        addCloseButton()
        setSearchViewMargin()
        setTypeFace(context.getInterFontTypeFace())
        preventFullScreenKeyboard()
        setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus)
                view.showSoftKeyboard()
        }
    }

    private fun addCloseButton() = closeButton?.apply {
        val layoutParams: LinearLayout.LayoutParams =
            this.layoutParams as LinearLayout.LayoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        setImageResource(R.drawable.ic_close)
    }

    private fun preventFullScreenKeyboard() {
        searchText.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_SEARCH
    }

    private fun setSearchViewMargin() {
        val params = searchEditFrame.layoutParams as LinearLayout.LayoutParams
        params.setMargins(-12, 0, 30, 0)
        searchEditFrame.layoutParams = params
        maxWidth = Integer.MAX_VALUE
    }

    private fun setTypeFace(typeface: Typeface?) {
        searchText.textSize = 17f
        searchText.typeface = typeface
    }

    fun resetSearchView() {
        setQuery("", false)
        clearFocus()
    }
}