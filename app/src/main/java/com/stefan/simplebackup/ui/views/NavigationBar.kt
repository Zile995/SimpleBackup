package com.stefan.simplebackup.ui.views

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.doesMatchDestination
import com.stefan.simplebackup.utils.extensions.hide
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.show
import java.lang.ref.WeakReference

class NavigationBar(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : BottomNavigationView(context, attrs, defStyleAttr, defStyleRes) {

    var initialHeight: Int = 0
        private set

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.bottomNavigationStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        R.style.Widget_Design_BottomNavigationView
    )

    init {
        doOnLayout {
            initialHeight = height
        }
    }

    inline fun moveUp(
        animationDuration: Long = 250L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ): ObjectAnimator? {
        if (isVisible) return null
        return ObjectAnimator.ofFloat(this, "translationY", 0f).apply {
            duration = animationDuration
            doOnStart {
                show()
                doOnStart()
            }
            doOnEnd {
                doOnEnd()
            }
            Log.d("NavigationBar", "Moving up")
        }
    }

    inline fun moveDown(
        animationDuration: Long = 250L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(this, "translationY", initialHeight.toFloat()).apply {
            duration = animationDuration
            doOnStart {
                doOnStart()
            }
            doOnEnd {
                doOnEnd()
                hide()
            }
            Log.d("NavigationBar", "Moving down")
        }
    }

    inline fun navigateWithAnimation(
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
                setEnterAnim(R.animator.fragment_nav_enter)
                setExitAnim(R.animator.fragment_nav_exit)
                setPopEnterAnim(R.animator.fragment_nav_enter_pop)
                setPopExitAnim(R.animator.fragment_nav_exit_pop)
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
                        if (destination.doesMatchDestination(item.itemId)) {
                            item.isChecked = true
                        }
                    }
                }
            })
    }
}