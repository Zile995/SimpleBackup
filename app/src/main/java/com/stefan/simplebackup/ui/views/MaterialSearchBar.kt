package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished

class MaterialSearchBar(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialCardView(context, attrs, defStyleAttr) {

    val parentWidth get() = (parent as? View)?.width
    val parentHeight get() = (parent as? View)?.height

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.materialCardViewStyle
    )

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)

    }
    
    inline fun animateToParentSize(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ): AnimatorSet {
        Log.d("SearchBarAnimation", "Animating to parent size")
        return animateTo(
            toHeightValue = parentHeight ?: height,
            toWidthValue = parentWidth ?: width,
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
            post {
                radius = valueAnimator.animatedValue as Float
                requestLayout()
            }
        }
        return AnimatorSet().apply {
            playTogether(widthAnimator, heightAnimator, radiusAnimator)
            doOnStart {
                animationFinished = false
                doOnStart()
            }
            doOnEnd {
                animationFinished = true
                doOnEnd()
            }
        }
    }
}