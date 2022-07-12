package com.stefan.simplebackup.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.R
import androidx.appcompat.widget.SearchView
import com.stefan.simplebackup.utils.extensions.showKeyboard

class MaterialSearchView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : SearchView(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.searchViewStyle
    )

    init {
        setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.showKeyboard()
            }
        }
    }

    fun setTypeFace(typeface: Typeface?) {
        val searchText = findViewById<TextView>(R.id.search_src_text)
        searchText.typeface = typeface
        searchText.textSize = 18f
    }
}