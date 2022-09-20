package com.stefan.simplebackup.ui.views

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished

class MaterialSearchBar(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialCardView(context, attrs, defStyleAttr) {

    var initialWidth: Int = 0
        private set
    var initialHeight: Int = 0
        private set
    var initialRadius: Float = 0f
        private set

    val parentWidth get() = (parent as View).width
    val parentHeight get() = (parent as View).height

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.materialCardViewStyle
    )

    constructor(context: Context) : this(context, null)

    init {
        doOnLayout {
            initialWidth = width
            initialHeight = height
            initialRadius = radius
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (enabled)
            setRippleColorResource(R.color.cardViewRipple)
        else
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
    }

    inline fun animateToParentSize(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ): AnimatorSet {
        Log.d("SearchBarAnimation", "Animating to parent size")
        return animateTo(
            toHeightValue = parentHeight,
            toWidthValue = parentWidth,
            toRadius = 0f,
            doOnStart = {
                doOnStart.invoke()
            },
            doOnEnd = {
                doOnEnd.invoke()
            }
        )
    }

    inline fun animateTo(
        toHeightValue: Int,
        toWidthValue: Int,
        toRadius: Float,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ): AnimatorSet {
        val widthAnimator = ValueAnimator.ofInt(width, toWidthValue)
        val heightAnimator = ValueAnimator.ofInt(height, toHeightValue)
        val radiusAnimator = ValueAnimator.ofFloat(radius, toRadius)

        widthAnimator.addUpdateListener { valueAnimator ->
            layoutParams.width = valueAnimator.animatedValue as Int
        }
        heightAnimator.addUpdateListener { valueAnimator ->
            layoutParams.height = valueAnimator.animatedValue as Int
        }
        radiusAnimator.addUpdateListener { valueAnimator ->
            radius = valueAnimator.animatedValue as Float
            requestLayout()
        }
        return AnimatorSet().apply {
            playTogether(widthAnimator, heightAnimator, radiusAnimator)
            doOnStart {
                isEnabled = false
                animationFinished = false
                doOnStart()
            }
            doOnEnd {
                doOnEnd()
                isEnabled = true
                animationFinished = true
            }
        }
    }
}