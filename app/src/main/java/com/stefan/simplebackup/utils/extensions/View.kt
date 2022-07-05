package com.stefan.simplebackup.utils.extensions

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.annotation.IdRes
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
import com.google.android.material.card.MaterialCardView
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

inline fun BottomNavigationView.navigateWithAnimation(
    navController: NavController,
    args: Bundle? = null,
    crossinline doBeforeNavigating: () -> Unit = {}
) {
    val weakReference = WeakReference(this)
    setOnItemSelectedListener { item ->
        doBeforeNavigating()
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

fun View.withAnimation(
    shouldShow: Boolean = false,
    heightTranslation: Float = this.height.toFloat()
) {
    var translationValue = 0f
    if (!shouldShow) translationValue = heightTranslation
    ObjectAnimator.ofFloat(this, "translationY", translationValue).apply {
        duration = 200L
        start()
    }
}

fun MaterialCardView.withRadiusAnimation(
    fromHeightValue: Int,
    toHeightValue: Int,
    fromWidthValue: Int,
    toWidthValue: Int,
    duration: Long = 300L,
    interpolator: TimeInterpolator = DecelerateInterpolator(),
    savedCardViewRadius: Float = this.radius
) =
    this.animateViewToParent(
        fromHeightValue,
        toHeightValue,
        fromWidthValue,
        toWidthValue,
        duration
    ) {
        val radiusAnimator =
            if (savedCardViewRadius != this.radius)
                ValueAnimator.ofFloat(0f, savedCardViewRadius)
            else
                ValueAnimator.ofFloat(savedCardViewRadius, 0f)
        radiusAnimator.duration = duration
        radiusAnimator.interpolator = interpolator
        radiusAnimator.addUpdateListener {
            this.radius = it.animatedValue as Float
            this.requestLayout()
        }
        radiusAnimator.start()
    }

fun View.animateViewToParent(
    fromHeightValue: Int,
    toHeightValue: Int,
    fromWidthValue: Int,
    toWidthValue: Int,
    duration: Long = 300L,
    interpolator: TimeInterpolator = DecelerateInterpolator(),
    applyWith: () -> Unit
) {
    val target = this
    val heightAnimator = ValueAnimator.ofInt(fromHeightValue, toHeightValue)
    val widthAnimator = ValueAnimator.ofInt(fromWidthValue, toWidthValue)
    heightAnimator.duration = duration
    widthAnimator.duration = duration
    heightAnimator.interpolator = interpolator
    widthAnimator.interpolator = interpolator
    heightAnimator.addUpdateListener { animation ->
        target.layoutParams.height = animation.animatedValue as Int
        target.requestLayout()
    }
    widthAnimator.addUpdateListener {
        target.layoutParams.width = it.animatedValue as Int
        target.requestLayout()
    }
    heightAnimator.start()
    widthAnimator.start()
    applyWith()
}