package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.doOnPreDraw
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.stefan.simplebackup.R

class MainFloatingButton(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : ExtendedFloatingActionButton(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.extendedFloatingActionButtonStyle
    )

    init {
        doOnPreDraw {
            shrink()
            hide()
            text = null
        }
    }

    fun changeOnSelection(isSelected: Boolean) {
        if (!isShown) show()
        if (isSelected) {
            setText(R.string.configure)
            setIconResource(R.drawable.ic_configure)
        } else {
            shrink()
            if (text != null) text = null
            setIconResource(R.drawable.ic_arrow_up)
        }
    }


}