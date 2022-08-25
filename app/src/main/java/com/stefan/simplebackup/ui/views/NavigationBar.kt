package com.stefan.simplebackup.ui.views

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.doesMatchDestination
import java.lang.ref.WeakReference

class NavigationBar(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : BottomNavigationView(context, attrs, defStyleAttr, defStyleRes) {

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

    inline fun moveVertically(
        value: Float = height.toFloat(),
        animationDuration: Long = 300L,
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        if (!isLaidOut) return
        ObjectAnimator.ofFloat(this, "translationY", value).apply {
            duration = animationDuration
            interpolator = LinearInterpolator()
            doOnStart {
                doOnStart()
            }
            doOnEnd {
                doOnEnd()
            }
            start()
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
                        if (destination.doesMatchDestination(item.itemId)) {
                            item.isChecked = true
                        }
                    }
                }
            })
    }
}