package com.stefan.simplebackup.utils.extensions

import android.animation.*
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.core.animation.doOnStart
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.ui.views.MaterialSearchBar

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
        .diskCachePolicy(CachePolicy.ENABLED)
        .target(this)
        .build()
    imageLoader.enqueue(request)
}

fun NavDestination.doesMatchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { navDestination ->
        navDestination.id == destId
    }

fun View.changeBackgroundColor(context: Context, @ColorRes color: Int) {
    setBackgroundColor(
        context.getColorFromResource(color)
    )
}

fun View.moveVertically(animationDuration: Long = 300L, value: Float) {
    ObjectAnimator.ofFloat(this, "translationY", value).apply {
        duration = animationDuration
        start()
    }
}

inline fun View.fadeIn(
    animationDuration: Long = 300L,
    crossinline onAnimationCancel: () -> Unit = {},
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

            override fun onAnimationPause(animation: Animator?) {
                onAnimationEnd.invoke()
            }

            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnd.invoke()
            }

            override fun onAnimationCancel(animation: Animator?) {
                fadeOut()
                onAnimationCancel.invoke()
            }
        })
}

inline fun View.fadeOut(
    animationDuration: Long = 300L,
    crossinline onAnimationCancel: () -> Unit = {},
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

            override fun onAnimationCancel(animation: Animator?) {
                onAnimationCancel.invoke()
            }
        })
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(findFocus(), 0)
}