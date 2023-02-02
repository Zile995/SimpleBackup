package com.stefan.simplebackup.utils.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.viewpager2.widget.ViewPager2
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.Disposable
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

fun View.getAttributeResourceId(@AttrRes attrId: Int) = TypedValue().run {
    context.theme.resolveAttribute(
        attrId,
        this,
        true
    )
    resourceId
}

fun ImageView.loadBitmap(byteArray: ByteArray): Disposable {
    val imageLoader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(byteArray)
        .crossfade(false)
        .target(this)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
    return imageLoader.enqueue(request)
}

fun ViewPager2.findCurrentFragment(fragmentManager: FragmentManager): Fragment? =
    fragmentManager.findFragmentByTag("f$currentItem")

fun NavDestination.doesMatchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { navDestination ->
        navDestination.id == destId
    }

fun View.changeBackgroundColor(context: Context, @ColorRes color: Int) =
    setBackgroundColor(context.getResourceColor(color))

inline fun View.fadeIn(
    animationDuration: Long = 250L,
    crossinline onAnimationEnd: () -> Unit = {}
) {
    if (isVisible) return
    animate()
        .alpha(1f)
        .setDuration(animationDuration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                show()
            }

            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd.invoke()
            }
        })
}

inline fun View.fadeOut(
    animationDuration: Long = 250L,
    crossinline onAnimationEnd: () -> Unit = {}
) {
    if (!isVisible) return
    animate()
        .alpha(0f)
        .setDuration(animationDuration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                hide()
                onAnimationEnd.invoke()
            }
        })
}

fun View.showSoftKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(findFocus(), 0)
}

fun View.hideSoftKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}