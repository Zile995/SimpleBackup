package com.stefan.simplebackup.ui.views

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
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

    fun moveDown(): ObjectAnimator =
        ObjectAnimator.ofFloat(this, "translationY", height.toFloat()).apply {
            Log.d("NavigationBar", "Moving down")
        }

    /**
     * Setup [NavigationBar] navigation with [NavController].
     *
     * @param onNavigate Indicates if [MenuItem] is reselected.
     * Returns true when it can be navigated.
     *
     * @param customNavigationOptions Provides custom options for the current [NavOptions] builder.
     * Contains the current [MenuItem] ID.
     */
    inline fun setupNavigation(
        navController: NavController,
        args: Bundle? = null,
        crossinline onNavigate: (Boolean) -> Boolean = { true },
        crossinline customNavigationOptions: (NavOptions.Builder.(MenuItem) -> Unit) = {}
    ) {
        var reselectedItemId = selectedItemId
        val weakNavBarReference = WeakReference(this)

        // On reselected item listener
        setOnItemReselectedListener { reselectedItem -> reselectedItemId = reselectedItem.itemId }

        // On selected item listener
        setOnItemSelectedListener { item ->
            val isReselected = item.itemId == reselectedItemId
            if (!onNavigate(isReselected)) return@setOnItemSelectedListener false
            val navOptions = NavOptions.Builder().apply {
                setRestoreState(true)
                setLaunchSingleTop(false)
                setPopUpTo(
                    navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = true
                )
                customNavigationOptions.invoke(this, item)
            }
            navController.navigate(item.itemId, args, navOptions.build())
            true
        }

        // Check the item with id of destination
        navController.addOnDestinationChangedListener(
            object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: Bundle?
                ) {
                    val navBar = weakNavBarReference.get()
                    if (navBar == null) {
                        navController.removeOnDestinationChangedListener(this)
                        return
                    }
                    navBar.menu.forEach { item ->
                        if (destination.doesMatchDestination(item.itemId)) {
                            item.isChecked = true
                        }
                    }
                }
            })
    }
}