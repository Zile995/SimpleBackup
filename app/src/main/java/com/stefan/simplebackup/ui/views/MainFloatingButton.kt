package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.doOnPreDraw
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.moveVertically

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
            text = null
            hide()
            shrink()
        }
    }

    fun changeOnSelection(isSelected: Boolean) {
        if (isSelected) {
            hide()
            setText(R.string.configure)
            setIconResource(R.drawable.ic_configure)
            show()
        } else {
            moveVertically(
                0L,
                resources.getDimensionPixelSize(R.dimen.bottom_navigation_height).toFloat()
            )
            show()
            moveVertically(300L, 0f)
            if (isExtended) shrink()
            if (text != null) text = null
            setIconResource(R.drawable.ic_arrow_up)
        }
    }


}