package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
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

    inline fun animateToInitialSize(
        duration: Long = 300L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        doOnPreDraw {
            if (height == initialHeight || width == initialWidth || radius == initialRadius) return@doOnPreDraw
            Log.d("SearchBarAnimation", "Animating to initial size")
            animateTo(
                toHeightValue = initialHeight,
                toWidthValue = initialWidth,
                duration = duration,
                doOnStart = {
                    doOnStart.invoke()
                },
                doOnEnd = {
                    doOnEnd.invoke()
                }
            )
        }
    }

    inline fun animateToParentSize(
        duration: Long = 300L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        doOnPreDraw {
            if (height == parentHeight || width == parentWidth) return@doOnPreDraw
            Log.d("SearchBarAnimation", "Animating to parent size")
            animateTo(
                toHeightValue = parentHeight,
                toWidthValue = parentWidth,
                duration = duration,
                doOnStart = {
                    doOnStart.invoke()
                },
                doOnEnd = {
                    doOnEnd.invoke()
                }
            )
        }
    }

    inline fun animateTo(
        toHeightValue: Int,
        toWidthValue: Int,
        duration: Long = 300L,
        interpolator: TimeInterpolator = DecelerateInterpolator(),
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        isEnabled = false
        animationFinished = false

        val widthAnimator = ValueAnimator.ofInt(width, toWidthValue)
        val heightAnimator = ValueAnimator.ofInt(height, toHeightValue)
        val radiusAnimator =
            if (initialRadius != radius)
                ValueAnimator.ofFloat(radius, initialRadius)
            else
                ValueAnimator.ofFloat(radius, 0f)

        widthAnimator.addUpdateListener { valueAnimator ->
            layoutParams.width = valueAnimator.animatedValue as Int
        }
        heightAnimator.addUpdateListener { valueAnimator ->
            layoutParams.height = valueAnimator.animatedValue as Int
            requestLayout()
        }
        radiusAnimator.addUpdateListener { valueAnimator ->
            valueAnimator.doOnEnd {
                doOnEnd.invoke()
                isEnabled = true
                animationFinished = true
            }
            radius = valueAnimator.animatedValue as Float
        }

        doOnStart.invoke()
        val animatorSet = AnimatorSet().apply {
            this.interpolator = interpolator
            this.duration = duration
            playTogether(widthAnimator, heightAnimator, radiusAnimator)
        }
        animatorSet.start()
    }

}