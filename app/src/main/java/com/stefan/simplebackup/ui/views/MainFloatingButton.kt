package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.stefan.simplebackup.R

class MainFloatingButton(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int
) : ExtendedFloatingActionButton(context, attrs, defStyleAttr) {

    var hidePermanently = false
        set(value) {
            field = value
            if (value) {
                hide()
                setOnClickListener(null)
            } else {
                shrink()
            }
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context, attrs, com.google.android.material.R.attr.extendedFloatingActionButtonStyle
    )

    override fun show() {
        if (hidePermanently) return
        super.show()
    }

    fun changeOnSelection(isSelected: Boolean, isHomeFragment: Boolean) = post {
        when {
            isSelected -> {
                if (isHomeFragment) {
                    setText(R.string.configure)
                    setIconResource(R.drawable.ic_configure)
                } else {
                    setText(R.string.restore)
                    setIconResource(R.drawable.ic_restore)
                }
                show()
            }

            else -> setDefaultState()
        }
    }

    private fun setDefaultState() {
        shrink()
        text = null
        setIconResource(R.drawable.ic_arrow_up)
    }
}