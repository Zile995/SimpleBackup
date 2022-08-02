package com.stefan.simplebackup.ui.views

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.google.android.material.R
import com.google.android.material.bottomnavigation.BottomNavigationView
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
                setEnterAnim(com.stefan.simplebackup.R.anim.fragment_enter)
                setExitAnim(com.stefan.simplebackup.R.anim.fragment_exit)
                setPopEnterAnim(com.stefan.simplebackup.R.anim.fragment_enter_pop)
                setPopExitAnim(com.stefan.simplebackup.R.anim.fragment_exit_pop)
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