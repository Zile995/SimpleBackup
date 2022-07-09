package com.stefan.simplebackup.utils.extensions

import android.animation.*
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.widget.SearchView
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import java.lang.ref.WeakReference

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


fun SearchView.setTypeFace(typeface: Typeface?) {
    val searchText = this.findViewById<View>(androidx.appcompat.R.id.search_src_text) as TextView
    searchText.typeface = typeface
}

inline fun BottomNavigationView.navigateWithAnimation(
    navController: NavController,
    args: Bundle? = null,
    crossinline doBeforeNavigating: () -> Boolean = { true }
) {
    setOnItemSelectedListener { item ->
        val shouldNavigate = doBeforeNavigating()
        if (!shouldNavigate) return@setOnItemSelectedListener false
        val navOptions = NavOptions.Builder().apply {
            setLaunchSingleTop(true)
            setRestoreState(true)
            setEnterAnim(R.anim.fragment_enter)
            setExitAnim(R.anim.fragment_exit)
            setPopEnterAnim(R.anim.fragment_enter_pop)
            setPopExitAnim(R.anim.fragment_exit_pop)
            setPopUpTo(
                navController.graph.startDestinationId,
                inclusive = false,
                saveState = true
            )
        }
        navController.navigate(item.itemId, args, navOptions.build())
        true
    }
    val weakReference = WeakReference(this)
    navController.addOnDestinationChangedListener(
        object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?
            ) {
                val view = weakReference.get()
                if (view == null) {
                    navController.removeOnDestinationChangedListener(this)
                    return
                }
                view.menu.forEach { item ->
                    if (destination.matchDestination(item.itemId)) {
                        item.isChecked = true
                    }
                }
            }
        })
}

fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { it.id == destId }

fun RecyclerView.smoothSnapToPosition(
    position: Int,
    snapMode: Int = LinearSmoothScroller.SNAP_TO_START
) {
    val scrollDuration = 380f
    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int = snapMode
        override fun getHorizontalSnapPreference(): Int = snapMode
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
            return scrollDuration / computeVerticalScrollRange()
        }
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

fun RecyclerView.onSaveRecyclerViewState(saveState: (Parcelable) -> Unit) {
    layoutManager?.onSaveInstanceState()?.let { stateParcelable ->
        saveState(stateParcelable)
    }
}

fun RecyclerView.onRestoreRecyclerViewState(parcelable: Parcelable?) =
    parcelable?.let { stateParcelable ->
        layoutManager?.onRestoreInstanceState(stateParcelable)
    }

fun RecyclerView.canScrollUp() = canScrollVertically(-1)

fun RecyclerView.canScrollDown() = canScrollVertically(1)

fun RecyclerView.hideAttachedButton(floatingButton: FloatingActionButton) {
    var checkOnAttach = true
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (checkOnAttach) {
                val linearLayoutManager = layoutManager as LinearLayoutManager
                val firstItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                val lastItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                if ((firstItemPosition == 0 || lastItemPosition == linearLayoutManager.itemCount - 1)
                    && floatingButton.isShown
                ) {
                    floatingButton.hide()
                }
                checkOnAttach = false
            }

            if (dy > 0 && floatingButton.isShown) {
                floatingButton.hide()

            } else if (dy < 0 && !floatingButton.isShown) {
                floatingButton.show()
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (!canScrollUp() || !canScrollDown()) floatingButton.hide()
        }
    })
}

fun View.changeBackgroundColor(context: Context, @ColorRes color: Int) {
    setBackgroundColor(
        context.getColorFromResource(color)
    )
}

fun View.moveToTheLeft(animationDuration: Long = 300L, value: Float) {
    ObjectAnimator.ofFloat(this, "translationX", value).apply {
        duration = animationDuration
        start()
    }
}

fun View.moveToTheRight(animationDuration: Long = 300L, value: Float) {
    ObjectAnimator.ofFloat(this, "translationX", value).apply {
        duration = animationDuration
        start()
    }
}

inline fun View.fadeIn(
    animationDuration: Long = 300L,
    crossinline onAnimationEnd: () -> Unit = {},
    crossinline onAnimationCancel: () -> Unit = {}
) {
    animate()
        .alpha(1f)
        .setDuration(animationDuration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                show()
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                onAnimationEnd.invoke()
            }

            override fun onAnimationCancel(animation: Animator?) {
                super.onAnimationCancel(animation)
                fadeOut {
                    onAnimationCancel.invoke()
                }
            }
        })
}

inline fun View.fadeOut(
    animationDuration: Long = 300L,
    crossinline onAnimationEnd: () -> Unit = {}
) {
    animate()
        .alpha(0f)
        .setDuration(animationDuration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                hide()
                onAnimationEnd.invoke()
            }
        })
}

inline fun ViewGroup.animateTo(
    fromHeightValue: Int,
    toHeightValue: Int,
    fromWidthValue: Int,
    toWidthValue: Int,
    duration: Long = 300L,
    interpolator: TimeInterpolator = DecelerateInterpolator(),
    applyWith: () -> Unit = {}
) {
    val heightAnimator = ValueAnimator.ofInt(fromHeightValue, toHeightValue)
    val widthAnimator = ValueAnimator.ofInt(fromWidthValue, toWidthValue)
    heightAnimator.duration = duration
    widthAnimator.duration = duration
    heightAnimator.interpolator = interpolator
    widthAnimator.interpolator = interpolator
    heightAnimator.addUpdateListener { animation ->
        layoutParams.height = animation.animatedValue as Int
        requestLayout()
    }
    widthAnimator.addUpdateListener {
        layoutParams.width = it.animatedValue as Int
        requestLayout()
    }
    heightAnimator.start()
    widthAnimator.start()
    applyWith()
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(findFocus(), 0)
}