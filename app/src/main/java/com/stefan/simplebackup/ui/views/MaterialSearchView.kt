package com.stefan.simplebackup.ui.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnPreDraw
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.getInterFontTypeFace
import com.stefan.simplebackup.utils.extensions.showKeyboard

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
        doOnPreDraw {
            addCloseButton()
            setSearchViewMargin(-20)
            setTypeFace(context.getInterFontTypeFace())
        }
        setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.showKeyboard()
            }
        }
    }

    private fun addCloseButton() = closeButton?.apply {
        setImageResource(R.drawable.ic_close)
        val layoutParams: LinearLayout.LayoutParams =
            this.layoutParams as LinearLayout.LayoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    @Suppress("SameParameterValue")
    fun setSearchViewMargin(leftMargin: Int) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        params.setMargins(leftMargin, 0, 0, 0)
        searchEditFrame.layoutParams = params
    }

    private fun setTypeFace(typeface: Typeface?) {
        searchText.typeface = typeface
        searchText.textSize = 17f
    }

    fun resetSearchView() {
        clearFocus()
        setQuery("", false)
    }
}