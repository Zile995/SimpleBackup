package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.doOnPreDraw
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
            }
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context, attrs, R.attr.extendedFloatingActionButtonStyle
    )

    override fun show() {
        if (hidePermanently) return
        super.show()
    }

    fun changeOnSelection(isSelected: Boolean) = if (isSelected) show() else setDefaultState()

    fun changeOnHomeFragment(isHomeFragment: Boolean) = post {
        if (isHomeFragment) {
            setIconResource(R.drawable.ic_configure)
            setText(R.string.configure)
        } else {
            setIconResource(R.drawable.ic_restore)
            setText(R.string.restore)
        }
    }

    private fun setDefaultState() {
        shrink()
        text = null
        setIconResource(R.drawable.ic_arrow_up)
    }
}