package com.stefan.simplebackup.ui.views

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.animateTo

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

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.materialCardViewStyle
    )

    constructor(context: Context) : this(context, null)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (enabled)
            setRippleColorResource(R.color.cardViewRipple)
        else
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
    }

    fun expandToParentView() {
        radius = 0f
        layoutParams.height = (parent as View).height
        layoutParams.width = (parent as View).width
        (layoutParams as MarginLayoutParams).leftMargin = 0
        (layoutParams as MarginLayoutParams).rightMargin = 0
        requestLayout()
    }

    fun saveTheCurrentDimensions() {
        cachedHeight = height
        cachedWidth = width
        cachedRadius = radius
        cachedLeftMargin = marginLeft
        cachedRightMargin = marginRight
    }

    inline fun animateToInitialSize(
        animationDuration: Long = 300L,
        crossinline doOnAnimationStart: () -> Unit = {},
        crossinline doOnAnimationEnd: () -> Unit = {}
    ) {
        animateTo(
            toHeightValue = cachedHeight,
            toWidthValue = cachedWidth,
            savedCardViewRadius = cachedRadius,
            animationDuration = animationDuration,
            doOnAnimationStart = {
                doOnAnimationStart.invoke()
            },
            doOnAnimationEnd = {
                doOnAnimationEnd.invoke()
            }
        )
    }

    inline fun animateTo(
        toHeightValue: Int,
        toWidthValue: Int,
        animationDuration: Long = 300L,
        interpolator: TimeInterpolator = DecelerateInterpolator(),
        savedCardViewRadius: Float = this.radius,
        crossinline doOnAnimationStart: () -> Unit = {},
        crossinline doOnAnimationEnd: () -> Unit = {}
    ) {
        doOnAnimationStart.invoke()
        animateTo(
            height,
            toHeightValue,
            width,
            toWidthValue,
            animationDuration
        ) {
            val radiusAnimator =
                if (savedCardViewRadius != radius)
                    ValueAnimator.ofFloat(0f, savedCardViewRadius)
                else
                    ValueAnimator.ofFloat(savedCardViewRadius, 0f)
            radiusAnimator.duration = animationDuration
            radiusAnimator.interpolator = interpolator
            radiusAnimator.addUpdateListener { valueAnimator ->
                radius = valueAnimator.animatedValue as Float
                requestLayout()
                valueAnimator.doOnStart {
                    doOnAnimationStart.invoke()
                }
                valueAnimator.doOnEnd {
                    doOnAnimationEnd.invoke()
                }
            }
            radiusAnimator.start()
        }
    }
}