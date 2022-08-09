package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginBottom
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.appbar.AppBarLayout
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.NotificationReceiver
import com.stefan.simplebackup.data.receivers.PackageReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.fragments.viewpager.BaseViewPagerFragment
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.AppBarLayoutStateChangedListener
import com.stefan.simplebackup.ui.views.MainActivityAnimator
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.ui.views.MaterialSearchView
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.root.RootChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {
    // Binding properties
    private val binding by viewBinding(ActivityMainBinding::inflate)

    // NavController for fragments
    private lateinit var navController: NavController

    // ViewModel
    private val mainViewModel: MainViewModel by viewModels {
        ViewModelFactory(application as MainApplication)
    }

    // Create RootChecker class instance lazily
    private val rootChecker by lazy { RootChecker(applicationContext) }

    // Create materialSearchBarAnimator
    private val mainActivityAnimator by lazy {
        MainActivityAnimator(WeakReference(this), WeakReference(binding))
    }

    // Broadcast receivers
    private val packageReceiver: PackageReceiver by lazy {
        PackageReceiver(
            mainViewModel,
            mainViewModel.viewModelScope
        )
    }
    private val notificationReceiver: NotificationReceiver by lazy {
        NotificationReceiver()
    }

    // Flags
    private var isSubmitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareLayoutWhenSearching()
        setContentView(binding.root)
        setNavController()
        binding.apply {
            bindViews()
            initObservers()
        }
        registerReceivers()
        setRootDialogs()
    }

    override fun onBackPressed() {
        if (animationFinished)
            super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        mainActivityAnimator.shrinkSearchBarToInitialSize()
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isSubmitted = savedInstanceState.getBoolean("isSubmitted")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("isSubmitted", isSubmitted)
    }

    private fun prepareLayoutWhenSearching() {
        mainActivityAnimator.prepareWhenSearching(mainViewModel.isSearching.value)
    }

    private fun setNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.doesMatchDestination(R.id.search_action)) {
                mainViewModel.setSearching(true)
            } else {
                mainViewModel.setSearching(false)
            }
        }
    }

    private fun navigateToSearchFragment() {
        navController.navigate(R.id.search_action, null, navOptions {
            anim {
                enter = R.anim.fade_enter
                exit = R.anim.fade_exit
                popEnter = R.anim.fade_enter_pop
                popExit = R.anim.fade_exit_pop
            }
        })
    }

    private fun ActivityMainBinding.bindViews() {
        window.setBackgroundDrawableResource(R.color.background)
        bindAppBarLayout()
        bindToolBar()
        bindSearchBar()
        bindBottomNavigationView()
    }

    private fun ActivityMainBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                mainViewModel.apply {
                    launch {
                        isSearching.collect { isSearching ->
                            if (isSearching) {
                                navigationBar.fadeOut(mainActivityAnimator.animationDuration)
                                floatingButton.fadeOut(mainActivityAnimator.animationDuration)
                                floatingButton.setOnClickListener(null)
                            } else {
                                floatingButton.isVisible = false
                                navigationBar.fadeIn(mainActivityAnimator.animationDuration)
                            }
                            materialToolbar.changeWhenSearching(isSearching,
                                setNavigationOnClickListener = {
                                    onSupportNavigateUp()
                                })
                        }
                    }
                    isSelected.collect { isSelected ->
                        expandAppBarLayout(isSelected)
                        if (isSelected) {
                            navigationBar.isClickable = false
                            navigationBar.moveVertically(
                                mainActivityAnimator.animationDuration,
                                navigationBar.height.toFloat(),
                                doOnStart = {
                                    val layoutParams =
                                        navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
                                    layoutParams.bottomMargin = appBarLayout.height
                                    navHostContainer.layoutParams = layoutParams
                                    navHostContainer.requestLayout()
                                })
                        } else {
                            if (navigationBar.marginBottom == navigationBar.height) return@collect
                            navigationBar.moveVertically(
                                mainActivityAnimator.animationDuration,
                                0f
                            ) {
                                val layoutParams =
                                    navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
                                layoutParams.bottomMargin = navigationBar.height
                                navHostContainer.layoutParams = layoutParams
                                navHostContainer.requestLayout()
                            }
                        }
                        floatingButton.changeOnSelection(isSelected)
                        materialToolbar.changeOnSelection(isSelected, setSelectionMode)
                        if (isSelected) {
                            if (isAppBarCollapsed) delay(250)
                            mainActivityAnimator.animateSearchBarOnSelection()
                        } else {
                            if (isAppBarCollapsed) delay(250)
                            mainActivityAnimator.shrinkSearchBarToInitialSize()
                        }
                    }
                }
            }
        }
    }

    private fun expandAppBarLayout(shouldExpand: Boolean) {
        binding.apply {
            val baseFragment =
                supportFragmentManager.getCurrentFragment() as BaseViewPagerFragment<*>
            val shouldMoveFragmentUp = baseFragment.shouldMoveFragmentUp() ?: false
            if (shouldExpand) {
                animationFinished = false
                appBarLayout.setExpanded(shouldExpand, true)
            } else {
                if (shouldMoveFragmentUp) {
                    Log.d("AppBarExpanding", "Calling again onCollapse")
                    appBarLayout.setExpanded(!shouldMoveFragmentUp, true)
                } else if (mainViewModel.isAppBarExpanded != shouldExpand)
                    appBarLayout.setExpanded(!shouldExpand, true)
            }
        }
    }

    private fun ActivityMainBinding.bindAppBarLayout() {
        appBarLayout.setExpanded(mainViewModel.isAppBarExpanded, false)
        root.doOnPreDraw {
            appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangedListener() {
                override fun onStateChanged(appBarLayout: AppBarLayout, state: AppBarLayoutState) {
                    when (state) {
                        AppBarLayoutState.EXPANDED -> mainViewModel.isAppBarExpanded = true
                        AppBarLayoutState.COLLAPSED -> mainViewModel.isAppBarExpanded = false
                        else -> mainViewModel.isAppBarExpanded = false
                    }
                }
            })


            val layoutParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val layoutBehavior = layoutParams.behavior as AppBarLayout.Behavior
            layoutParams.behavior = layoutBehavior
            layoutBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return !(mainViewModel.isSelected.value || mainViewModel.isSearching.value)
                }
            })
        }
    }

    private fun ActivityMainBinding.bindSearchBar() {
        expandTheSearchBarWhenSearching()
        materialSearchBar.setOnClickListener {
            navigateToSearchFragment()
            mainActivityAnimator.animateSearchBarOnClick()
        }
    }

    private fun ActivityMainBinding.expandTheSearchBarWhenSearching() {
        root.doOnPreDraw {
            if (mainViewModel.isSearching.value || mainViewModel.isSelected.value) {
                // If we are searching, fill the parent (we need reverse animations later)
                materialSearchBar.fillTheParent()
            }
        }
    }

    private fun ActivityMainBinding.bindToolBar() {
        materialToolbar.title = getString(R.string.search_for_apps)
        materialToolbar.tooltipText = null
        materialToolbar.inflateMenu(R.menu.main_tool_bar)
        materialToolbar.setNavigationIcon(R.drawable.ic_search)
        materialToolbar.setNavigationOnClickListener {
            materialSearchBar.performClick()
        }
        materialToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    val searchView = menuItem?.actionView as MaterialSearchView
                }
                R.id.add_to_favorites -> {
                    mainViewModel.changeFavorites()
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
            true
        }
    }

    private fun ActivityMainBinding.bindBottomNavigationView() {
        navigationBar.navigateWithAnimation(navController, doBeforeNavigating = {
            floatingButton.setOnClickListener(null)
            return@navigateWithAnimation !mainViewModel.isSearching.value || !mainViewModel.isSelected.value
        })
    }

    private fun registerReceivers() {
        registerReceiver(packageReceiver, intentFilter(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED
        ) {
            addDataScheme("package")
        })
        registerReceiver(notificationReceiver, intentFilter(ACTION_WORK_FINISHED))
    }

    private fun setRootDialogs() {
        launchOnViewLifecycle {
            if (!isSubmitted) {
                hasRootAccess()
                isSubmitted = true
            }
            if (!PreferenceHelper.isRootChecked && rootChecker.isRooted()
                && !PreferenceHelper.isRootGranted
            ) {
                rootDialog(
                    getString(R.string.root_detected),
                    getString(R.string.not_granted)
                )
            } else if (!PreferenceHelper.isRootChecked && !rootChecker.isRooted()
            ) {
                rootDialog(
                    getString(R.string.not_rooted),
                    getString(R.string.not_rooted_info)
                )
            }
        }
    }

    private suspend fun hasRootAccess() {
        if (rootChecker.hasRootAccess() == true) {
            PreferenceHelper.setRootGranted(true)
        } else {
            PreferenceHelper.setRootGranted(false)
        }
    }

    private suspend fun rootDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(getString(R.string.OK)) { dialog, _ ->
                dialog.cancel()
            }
        }
        PreferenceHelper.setRootChecked(true)
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.positiveDialog))
        }
        alert.show()
    }

    fun MainRecyclerView.controlFloatingButton() {
        binding.floatingButton.setOnClickListener {
            smoothSnapToPosition(0)
        }
        hideAttachedButton(binding.floatingButton)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers(packageReceiver, notificationReceiver)
    }
}


