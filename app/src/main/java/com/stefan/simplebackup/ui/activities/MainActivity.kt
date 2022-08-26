package com.stefan.simplebackup.ui.activities

import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
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
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.numberOfSelected
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.selectionFinished
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.AppBarLayoutStateChangedListener
import com.stefan.simplebackup.ui.views.MainActivityAnimator
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {
    // Binding properties
    private val binding by viewBinding(ActivityMainBinding::inflate)

    // NavController, used for navigation
    private lateinit var navController: NavController

    // ViewModel
    private val mainViewModel: MainViewModel by viewModels {
        ViewModelFactory(application as MainApplication)
    }

    // Create MainActivityAnimator class instance lazily
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
    private var shouldExit = false

    // Jobs
    private var delayedExitJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        if (!selectionFinished) {
            shouldExit = true
            mainViewModel.setSelectionMode(false)
            return
        }
        if (animationFinished)
            if (shouldExit) {
                launchOnViewLifecycle {
                    shouldExit = false
                    showToast(R.string.press_back_again)
                    delayedExitJob = launch {
                        delay(1500L)
                        shouldExit = true
                    }
                }
            } else
                super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.doesMatchDestination(R.id.search_action))
                mainViewModel.setSearching(true)
            else launchPostDelayed(50L) {
                mainViewModel.setSearching(false)
            }
            destination.doesMatchDestination(R.id.home).let { isHomeDestination ->
                shouldExit = isHomeDestination
                if (!isHomeDestination) {
                    delayedExitJob?.cancel()
                    delayedExitJob = null
                }
            }
        }
    }

    private fun navigateToSearchFragment() {
        val currentDestination = navController.currentDestination
        navController.navigate(R.id.search_action, null, navOptions {
            launchSingleTop = true
            anim {
                enter = R.animator.fragment_fade_enter
                exit = R.animator.fragment_fade_exit
                popEnter = R.animator.fragment_fade_enter
                popExit = R.animator.fragment_fade_exit
            }
            popUpTo(
                currentDestination?.id ?: navController.graph.startDestinationId
            ) {
                inclusive = false
                saveState = true
            }
        })
    }

    private fun ActivityMainBinding.bindViews() {
        window.setBackgroundDrawableResource(R.color.background)
        bindAppBarLayout()
        bindSearchBar()
        bindToolBar()
        bindBottomNavigationView()
    }

    private suspend fun ActivityMainBinding.observeNumberOfSelected(isSelected: Boolean) {
        if (!isSelected) return
        numberOfSelected.collect { numberOfItems ->
            when (numberOfItems) {
                0 -> {}
                1 -> materialToolbar.title = "$numberOfItems ${getString(R.string.item)}"
                else -> materialToolbar.title = "$numberOfItems ${getString(R.string.items)}"
            }
        }
    }

    private fun ActivityMainBinding.bindAppBarLayout() {
        appBarLayout.setExpanded(mainViewModel.isAppBarExpanded, false)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangedListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: AppBarLayoutState) {
                when (state) {
                    AppBarLayoutState.EXPANDED -> mainViewModel.isAppBarExpanded = true
                    AppBarLayoutState.COLLAPSED -> mainViewModel.isAppBarExpanded = false
                    else -> mainViewModel.isAppBarExpanded = false
                }
            }
        })
        root.doOnPreDraw {
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

    private fun ActivityMainBinding.bindSearchBar() = materialSearchBar.setOnClickListener {
        navigateToSearchFragment()
    }

    private fun ActivityMainBinding.bindToolBar() {
        materialToolbar.inflateMenu(R.menu.main_tool_bar)
        materialToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {

                }
                R.id.select_all -> {
                    getVisibleFragment()?.selectAllItems()
                }
                R.id.add_to_favorites -> {
                    mainViewModel.changeFavorites()
                }
                R.id.delete_backup -> {

                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
            true
        }
    }

    private fun ActivityMainBinding.bindBottomNavigationView() =
        navigationBar.navigateWithAnimation(navController, doBeforeNavigating = {
            floatingButton.setOnClickListener(null)
            return@navigateWithAnimation !(mainViewModel.isSearching.value
                    || mainViewModel.isSelected.value)
        })

    private fun ActivityMainBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                mainViewModel.apply {
                    launch {
                        isSearching.collect { isSearching ->
                            if (isSelected.value) return@collect
                            if (isSearching) {
                                floatingButton.hidePermanently = true
                                navigationBar.moveVertically(animationDuration = 250)
                                mainActivityAnimator.animateSearchBarOnClick()
                            } else {
                                floatingButton.hidePermanently = false
                                navigationBar.moveVertically(0f, animationDuration = 250)
                                mainActivityAnimator.shrinkSearchBarToInitialSize()
                            }
                            materialToolbar.changeOnSearch(isSearching,
                                setNavigationOnClickListener = {
                                    onSupportNavigateUp()
                                })
                        }
                    }
                    isSelected.collect { isSelected ->
                        if (isSearching.value) return@collect
                        launch {
                            observeNumberOfSelected(isSelected)
                        }
                        if (isSelected) {
                            mainActivityAnimator.animateSearchBarOnSelection()
                            navigationBar.moveVertically(
                                animationDuration = 200,
                                doOnStart = {
                                    val layoutParams =
                                        navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
                                    layoutParams.bottomMargin = appBarLayout.height
                                    navHostContainer.layoutParams = layoutParams
                                    navHostContainer.requestLayout()
                                })
                        } else {
                            navigationBar.moveVertically(
                                0f,
                                200
                            ) {
                                val layoutParams =
                                    navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
                                layoutParams.bottomMargin = navigationBar.height
                                navHostContainer.layoutParams = layoutParams
                                navHostContainer.requestLayout()
                                getVisibleFragment()?.fixRecyclerViewScrollPosition()
                            }
                            mainActivityAnimator.shrinkSearchBarToInitialSize()
                        }
                        expandAppBarLayout(isSelected)
                        floatingButton.changeOnSelection(isSelected)
                        materialToolbar.changeOnSelection(isSelected, setSelectionMode)
                    }
                }
            }
        }
    }

    private fun expandAppBarLayout(shouldExpand: Boolean) =
        binding.apply {
            if (shouldExpand) {
                animationFinished = false
                appBarLayout.setExpanded(shouldExpand, true)
            } else {
                if (getVisibleFragment()?.shouldMoveFragmentUp() == true) {
                    Log.d("AppBarLayout", "Collapsing the AppBarLayout")
                    appBarLayout.setExpanded(false, true)
                }
            }
        }

    private fun getVisibleFragment(): BaseFragment<*>? {
        val viewPagerFragment = supportFragmentManager.getCurrentVisibleViewPagerFragment()
        return viewPagerFragment?.getVisibleFragment()
    }

    private fun registerReceivers() {
        registerReceiver(
            packageReceiver, intentFilter(
                ACTION_PACKAGE_ADDED,
                ACTION_PACKAGE_REMOVED
            ) {
                addDataScheme("package")
            })
        registerReceiver(notificationReceiver, intentFilter(ACTION_WORK_FINISHED))
    }

    private fun setRootDialogs() {
        launchOnViewLifecycle {
            mainViewModel.onRootCheck(
                onRootNotGranted = {
                    rootDialog(
                        getString(R.string.root_detected),
                        getString(R.string.not_granted)
                    )
                },
                onDeviceNotRooted = {
                    rootDialog(
                        getString(R.string.not_rooted),
                        getString(R.string.not_rooted_info)
                    )
                })
        }
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