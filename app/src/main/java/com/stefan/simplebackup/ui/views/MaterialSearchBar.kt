package com.stefan.simplebackup.ui.views

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished

class MaterialSearchBar(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialCardView(context, attrs, defStyleAttr) {

    var cachedHeight: Int = 0
        private set
    var cachedWidth: Int = 0
        private set
    var cachedRadius: Float = 0f
        private set
    var cachedLeftMargin: Int = 0
        private set
    var cachedRightMargin: Int = 0
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
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cachedHeight = height
                cachedWidth = width
                cachedRadius = radius
                cachedLeftMargin = marginLeft
                cachedRightMargin = marginRight
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
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
        if (height == cachedHeight || width == cachedWidth || radius == cachedRadius) return
        Log.d("MainAnimatorSearchBar", "Animating to initial size")
        animateTo(
            toHeightValue = cachedHeight,
            toWidthValue = cachedWidth,
            duration = duration,
            doOnStart = {
                doOnStart.invoke()
            },
            doOnEnd = {
                doOnEnd.invoke()
            }
        )
    }

    inline fun animateToParentSize(
        duration: Long = 300L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        doOnPreDraw {
            if (height == parentHeight || width == parentWidth) return@doOnPreDraw
            Log.d("MainAnimatorSearchBar", "Animating to parent size")
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
        val widthAnimator = ValueAnimator.ofInt(width, toWidthValue)
        val heightAnimator = ValueAnimator.ofInt(height, toHeightValue)
        val radiusAnimator =
            if (cachedRadius != radius)
                ValueAnimator.ofFloat(radius, cachedRadius)
            else
                ValueAnimator.ofFloat(radius, 0f)
        widthAnimator.duration = duration
        radiusAnimator.duration = duration
        heightAnimator.duration = duration
        widthAnimator.interpolator = interpolator
        heightAnimator.interpolator = interpolator
        radiusAnimator.interpolator = interpolator
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
        isEnabled = false
        animationFinished = false
        doOnStart.invoke()
        radiusAnimator.start()
        heightAnimator.start()
        widthAnimator.start()
    }

}