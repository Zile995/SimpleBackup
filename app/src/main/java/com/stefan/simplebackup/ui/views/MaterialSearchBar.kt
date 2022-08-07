package com.stefan.simplebackup.ui.views

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R

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

    fun fillTheParent() {
        radius = 0f
        layoutParams.height = (parent as View).height
        layoutParams.width = (parent as View).width
        (layoutParams as MarginLayoutParams).leftMargin = 0
        (layoutParams as MarginLayoutParams).rightMargin = 0
        requestLayout()
    }

    inline fun animateToInitialSize(
        duration: Long = 300L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        if (height == cachedHeight || width == cachedWidth || radius == cachedRadius)
            return
        animateTo(
            toHeightValue = cachedHeight,
            toWidthValue = cachedWidth,
            duration = duration,
            doOnStart = {
                doOnStart.invoke()
            },
            doOnEnd = {
                doOnEnd.invoke()
                requestLayout()
            }
        )
    }

    inline fun animateToParentSize(
        duration: Long = 300L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        if (height == parentHeight || width == parentWidth || radius == 0f)
            return
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

    inline fun animateTo(
        toHeightValue: Int,
        toWidthValue: Int,
        duration: Long = 300L,
        interpolator: TimeInterpolator = DecelerateInterpolator(),
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        println("Calling main animateTo from searchBar")
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
            }
            radius = valueAnimator.animatedValue as Float
        }
        doOnStart.invoke()
        radiusAnimator.start()
        heightAnimator.start()
        widthAnimator.start()
    }

}