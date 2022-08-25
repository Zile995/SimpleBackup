package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.core.view.doOnPreDraw
import com.google.android.material.chip.ChipGroup
import com.stefan.simplebackup.R


class AnimatedChipGroup(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : ChipGroup(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.chipGroupStyle
    )

    init {
        doOnPreDraw {
            animateOnStart()
        }
    }

    private fun animateOnStart() {
        if (!isLaidOut) return
        val horizontalAnimator = ObjectAnimator.ofFloat(
            this,
            "translationY",
            0f
        )
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1.1f, 1f)

        val animatorSet = AnimatorSet().apply {
            duration = 300L
            playTogether(horizontalAnimator, scaleX, scaleY)
        }
        animatorSet.start()
    }

}