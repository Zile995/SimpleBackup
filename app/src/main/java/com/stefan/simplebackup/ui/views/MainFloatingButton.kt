package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.stefan.simplebackup.R

class MainFloatingButton(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : ExtendedFloatingActionButton(context, attrs, defStyleAttr) {

    var hidePermanently = false
        set(value) {
            field = value
            if (value) {
                hide()
                setOnClickListener(null)
            } else {
                doOnPreDraw {
                    shrink()
                }
            }
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.extendedFloatingActionButtonStyle
    )

    override fun show() {
        if (hidePermanently) return
        super.show()
    }

    fun changeOnSelection(isSelected: Boolean) {
        if (isSelected) {
            show()
            doOnLayout {
                setText(R.string.configure)
                setIconResource(R.drawable.ic_configure)
            }
        } else {
            doOnPreDraw {
                setDefaultState()
            }
        }
    }

    private fun setDefaultState() {
        shrink()
        text = null
        setIconResource(R.drawable.ic_arrow_up)
    }
}