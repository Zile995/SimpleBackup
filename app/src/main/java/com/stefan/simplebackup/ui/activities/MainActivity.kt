package com.stefan.simplebackup.ui.activities

import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
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
import com.stefan.simplebackup.ui.fragments.ConfigureSheetFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.HomeFragment
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.AppBarLayoutStateChangedListener
import com.stefan.simplebackup.ui.views.MainActivityAnimator
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.ui.views.SimpleMaterialToolbar
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


class MainActivity : BaseActivity() {
    // Binding properties
    private val binding by viewBinding(ActivityMainBinding::inflate)

    private val visibleFragment get() = getVisibleFragment()

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
            mainViewModel.viewModelScope,
            mainViewModel
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
        setRootDialogs()
        registerReceivers()
    }

    override fun onBackPress() {
        if (animationFinished) {
            if (navController.currentDestination?.doesMatchDestination(R.id.home) == false) {
                navController.popBackStack()
                return
            }
            if (!selectionFinished) {
                shouldExit = true
                mainViewModel.setSelectionMode(false)
                return
            }
            if (shouldExit && PreferenceHelper.shouldDoublePressToExit) {
                launchOnViewLifecycle {
                    shouldExit = false
                    showToast(R.string.press_back_again)
                    delayedExitJob = launchPostDelayed(1500L) { shouldExit = true }
                    delayedExitJob?.invokeOnCompletion { delayedExitJob = null }
                }
            } else
                super.onBackPress()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isSearchDestination = destination.doesMatchDestination(R.id.search_action)
            val isSettingsDestination = destination.doesMatchDestination(R.id.settings)
            mainViewModel.setSearching(isSearchDestination)
            mainViewModel.setSettingsDestination(isSettingsDestination)
            destination.doesMatchDestination(R.id.home).let { isHomeDestination ->
                shouldExit = isHomeDestination
                if (!isHomeDestination) delayedExitJob?.cancel()
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
            popUpTo(currentDestination?.id ?: navController.graph.startDestinationId) {
                inclusive = false
                saveState = false
            }
        })
    }

    private fun ActivityMainBinding.bindViews() {
        bindToolBar()
        bindSearchBar()
        bindAppBarLayout()
        bindFloatingButton()
        bindBottomNavigationView()
    }

    private suspend fun ActivityMainBinding.observeNumberOfSelected() {
        numberOfSelected.collect { numberOfItems ->
            materialToolbar.changeMenuItems(numberOfItems)
            when (numberOfItems) {
                0 -> {}
                1 -> {
                    materialToolbar.setCustomTitle("$numberOfItems ${getString(R.string.item)}")
                }
                else -> {
                    materialToolbar.setCustomTitle("$numberOfItems ${getString(R.string.items)}")
                }
            }
        }
    }

    private fun ActivityMainBinding.bindFloatingButton() {
        floatingButton.isVisible = mainViewModel.isButtonVisible
    }

    private fun SimpleMaterialToolbar.changeMenuItems(numberOfSelectedItems: Int) {
        doOnLayout {
            when {
                numberOfSelectedItems > 1 && visibleFragment is HomeFragment -> {
                    deleteItem?.isVisible = false
                }
                numberOfSelectedItems >= 1 -> {
                    changeOnFavorite(visibleFragment is FavoritesFragment)
                }
            }
        }
    }

    private fun ActivityMainBinding.bindAppBarLayout() {
        appBarLayout.setExpanded(mainViewModel.isAppBarExpanded, false)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangedListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: AppBarLayoutState) {
                when (state) {
                    AppBarLayoutState.EXPANDED -> mainViewModel.changeAppBarState(true)
                    AppBarLayoutState.COLLAPSED -> mainViewModel.changeAppBarState(false)
                    else -> mainViewModel.changeAppBarState(false)
                }
            }
        })
        root.doOnLayout {
            val layoutParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val layoutBehavior = layoutParams.behavior as AppBarLayout.Behavior
            layoutParams.behavior = layoutBehavior
            layoutBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return !(mainViewModel.isSelected.value
                            || mainViewModel.isSearching.value
                            || navController.currentDestination?.doesMatchDestination(R.id.settings) == true
                            || navController.currentDestination?.doesMatchDestination(R.id.search_action) == true)
                }
            })
        }
    }

    private fun ActivityMainBinding.bindSearchBar() = materialSearchBar.setOnClickListener {
        navigateToSearchFragment()
    }

    private fun ActivityMainBinding.bindToolBar() {
        materialToolbar.inflateMenu(R.menu.main_tool_bar)
        materialToolbar.setOnSearchAction()
        materialToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_to_favorites -> {
                    if (visibleFragment !is FavoritesFragment)
                        mainViewModel.addToFavorites()
                    else
                        mainViewModel.removeFromFavorites()
                }
                R.id.delete -> {
                    Log.d("Activity", "Setting up the delete action")
                    if (visibleFragment is HomeFragment) {
                        visibleFragment?.deleteSelectedItem()
                    }
                }
                R.id.select_all -> {
                    visibleFragment?.selectAllItems()
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
            true
        }
    }

    private fun SimpleMaterialToolbar.setOnSearchAction() {
        searchActionView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("MainActivity", "Search text = $query")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean =
                mainViewModel.findAppsByName(newText).run {
                    true
                }
        })
    }

    private fun ActivityMainBinding.bindBottomNavigationView() =
        navigationBar.navigateWithAnimation(navController,
            doBeforeNavigating = {
                visibleFragment?.stopScrolling()
                floatingButton.setOnClickListener(null)
                !(mainViewModel.isSearching.value
                        || mainViewModel.isSelected.value) && animationFinished
            },
            setCustomNavigationOptions = { menuItem ->
                if (menuItem.itemId == R.id.settings) {
                    setEnterAnim(R.anim.nav_default_enter_anim)
                    setExitAnim(R.anim.nav_default_exit_anim)
                    setPopEnterAnim(R.animator.fragment_nav_enter_pop)
                    setPopExitAnim(R.animator.fragment_nav_exit_pop)
                }
            })

    private fun ActivityMainBinding.initObservers() {
        launchOnViewLifecycle {
            launch {
                repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                    observeNumberOfSelected()
                }
            }
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                mainViewModel.apply {
                    launch {
                        isSelected.collect { isSelected ->
                            if (isSearching.value) return@collect
                            mainActivityAnimator.animateOnSelection(isSelected, setSelectionMode)
                        }
                    }
                    launch {
                        isSearching.collect { isSearching ->
                            if (isSelected.value) return@collect
                            mainActivityAnimator.animateOnSearch(isSearching)
                            resetSearchResult()
                        }
                    }
                    launch {
                        isSettingsDestination.collect { isSettingsDestination ->
                            if (isSearching.value || isSelected.value) return@collect
                            mainActivityAnimator.animateOnSettings(isSettingsDestination)
                        }
                    }
                }
            }
        }
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
        if (binding.floatingButton.hidePermanently) return
        binding.floatingButton.setOnClickListener {
            if (selectionFinished)
                smoothSnapToPosition(0)
            else {
                if (mainViewModel.selectionList.isNotEmpty())
                    ConfigureSheetFragment().show(supportFragmentManager, "configureSheetTag")
            }
        }
        hideAttachedButton(binding.floatingButton)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers(packageReceiver, notificationReceiver)
        mainViewModel.changeButtonVisibility(binding.floatingButton.isVisible)
    }
}