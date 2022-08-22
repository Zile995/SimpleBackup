package com.stefan.simplebackup.utils.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.postDelayed
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        when {
            value -> show()
            else -> hide()
        }
    }

fun ImageView.loadBitmap(byteArray: ByteArray) {
    val imageLoader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(byteArray)
        .crossfade(true)
        .target(this)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
    imageLoader.enqueue(request)
}

fun NavDestination.doesMatchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { navDestination ->
        navDestination.id == destId
    }

fun View.changeBackgroundColor(context: Context?, @ColorRes color: Int) {
    context?.let { safeContext ->
        setBackgroundColor(
            safeContext.getColorFromResource(color)
        )
    }
}

inline fun View.fadeIn(
    animationDuration: Long = 300L,
    crossinline onAnimationEnd: () -> Unit = {}
) {
    if (isVisible) return
    animate()
        .alpha(1f)
        .setDuration(animationDuration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                show()
            }

            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnd.invoke()
            }
        })
}

inline fun View.fadeOut(
    animationDuration: Long = 300L,
    crossinline onAnimationEnd: () -> Unit = {}
) {
    if (!isVisible) return
    animate()
        .alpha(0f)
        .setDuration(animationDuration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                hide()
                onAnimationEnd.invoke()
            }
        })
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(findFocus(), 0)
}