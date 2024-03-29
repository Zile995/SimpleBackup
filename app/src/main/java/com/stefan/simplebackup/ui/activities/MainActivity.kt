package com.stefan.simplebackup.ui.activities

import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.google.android.material.appbar.AppBarLayout
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.receivers.PackageReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.inSelection
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.numberOfSelected
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.CONFIGURE_SHEET_TAG
import com.stefan.simplebackup.ui.fragments.ConfigureSheetFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.HomeFragment
import com.stefan.simplebackup.ui.fragments.LocalFragment
import com.stefan.simplebackup.ui.fragments.viewpager.BaseViewPagerFragment
import com.stefan.simplebackup.ui.fragments.viewpager.HomeViewPagerFragment
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.MainViewModelFactory
import com.stefan.simplebackup.ui.viewmodels.UiAction
import com.stefan.simplebackup.ui.views.AppBarLayoutStateChangedListener
import com.stefan.simplebackup.ui.views.MainActivityAnimator
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.ui.views.SimpleMaterialToolbar
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.doesMatchDestination
import com.stefan.simplebackup.utils.extensions.getNavController
import com.stefan.simplebackup.utils.extensions.getVisibleFragment
import com.stefan.simplebackup.utils.extensions.intentFilter
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.launchPostDelayed
import com.stefan.simplebackup.utils.extensions.repeatOnCreated
import com.stefan.simplebackup.utils.extensions.repeatOnResumed
import com.stefan.simplebackup.utils.extensions.showToast
import com.stefan.simplebackup.utils.extensions.unregisterReceivers
import com.stefan.simplebackup.utils.extensions.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : BaseActivity() {
    // Binding property
    private val binding by viewBinding(ActivityMainBinding::inflate)

    // NavController, used for navigation
    private lateinit var navController: NavController

    // Root alert dialog reference
    private var _rootAlertDialog: AlertDialog? = null

    // ViewModel
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory().factory
    }

    // Create MainActivityAnimator class instance lazily
    private val mainActivityAnimator by lazy {
        MainActivityAnimator(WeakReference(this), WeakReference(binding))
    }

    // Broadcast receivers
    private val packageReceiver: PackageReceiver by lazy {
        PackageReceiver(
            mainViewModel.viewModelScope, mainViewModel
        )
    }

    // Notification permission launcher
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val signInIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                handleSignInIntent(signInData = result.data!!, onSuccess = {
                    onSuccessfullySignedIn()
                }, onFailure = {
                    showToast(R.string.unable_to_sign_in)
                    Log.e("GoogleSignIn", "${getString(R.string.unable_to_sign_in)} $it")
                })
            }
        }

    // Flags
    private var shouldExit = false

    // Jobs
    private var delayedExitJob: Job? = null

    // BaseFragment getter
    val currentlyVisibleBaseFragment: BaseFragment<*>?
        get() = when (val visibleFragment = supportFragmentManager.getVisibleFragment()) {
            is BaseFragment<*> -> visibleFragment
            is BaseViewPagerFragment<*> -> visibleFragment.getCurrentFragment()
            else -> null
        }

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
        requestNotificationPermission()
    }

    override fun onBackPress() {
        if (animationFinished) {
            if (inSelection) {
                shouldExit = true
                mainViewModel.setSelectionMode(false)
                return
            }
            if (navController.currentDestination?.doesMatchDestination(R.id.home) == false) {
                navController.popBackStack()
                return
            }
            if (shouldExit && PreferenceHelper.shouldDoublePressToExit) {
                launchOnViewLifecycle {
                    shouldExit = false
                    showToast(R.string.press_back_again)
                    delayedExitJob = launchPostDelayed(1500L) { shouldExit = true }
                    delayedExitJob?.invokeOnCompletion { delayedExitJob = null }
                }
            } else super.onBackPress()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setNavController() {
        navController = getNavController(R.id.nav_host_container)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isSearchDestination = destination.doesMatchDestination(R.id.search_action)
            val isSettingsDestination = destination.doesMatchDestination(R.id.settings)
            mainViewModel.action(
                UiAction.ChangeSearchBarState(
                    isSearchDestination, isSettingsDestination
                )
            )
            destination.doesMatchDestination(R.id.home).let { isHomeDestination ->
                shouldExit = isHomeDestination
                if (!isHomeDestination) delayedExitJob?.cancel()
            }
        }
    }

    private fun navigateToSearchFragment() =
        navController.navigate(R.id.search_action, null, navOptions {
            launchSingleTop = true
            anim {
                enter = R.animator.fragment_fade_enter
                exit = R.animator.fragment_fade_exit
                popEnter = R.animator.fragment_fade_enter
                popExit = R.animator.fragment_fade_exit
            }
        })

    private fun ActivityMainBinding.bindViews() {
        bindToolBar()
        bindSearchBar()
        bindAppBarLayout()
        bindNavigationBar()
        bindFloatingButton()
    }

    private suspend fun ActivityMainBinding.observeNumberOfSelected() {
        numberOfSelected.collectLatest { numberOfItems ->
            root.doOnLayout {
                materialToolbar.changeMenuItems(numberOfItems)
                when (numberOfItems) {
                    0 -> {}
                    1 -> materialToolbar.setCustomTitle("$numberOfItems ${getString(R.string.item)}")
                    else -> materialToolbar.setCustomTitle("$numberOfItems ${getString(R.string.items)}")
                }
            }
        }
    }

    private fun ActivityMainBinding.bindFloatingButton() {
        floatingButton.isVisible = mainViewModel.isAppBarExpanded
    }

    private fun SimpleMaterialToolbar.changeMenuItems(numberOfSelectedItems: Int) {
        when {
            numberOfSelectedItems > 0 && currentlyVisibleBaseFragment is LocalFragment -> {
                addToFavoritesItem?.isVisible = false
                deleteItem?.isVisible = true
            }

            numberOfSelectedItems > 1 && currentlyVisibleBaseFragment is HomeFragment -> {
                deleteItem?.isVisible = false
            }

            numberOfSelectedItems > 0 && supportFragmentManager.getVisibleFragment() is HomeViewPagerFragment -> {
                changeOnFavorite(isFavorite = currentlyVisibleBaseFragment is FavoritesFragment)
            }
        }
    }

    private fun ActivityMainBinding.bindAppBarLayout() {
        appBarLayout.setExpanded(mainViewModel.isAppBarExpanded, false)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangedListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: AppBarLayoutState) {
                val isExpanded = when (state) {
                    AppBarLayoutState.EXPANDED -> true
                    AppBarLayoutState.COLLAPSED -> false
                    else -> false
                }
                mainViewModel.action(UiAction.ChangeAppBarState(isExpanded = isExpanded))
            }
        })
        root.doOnLayout {
            val layoutParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val layoutBehavior = layoutParams.behavior as AppBarLayout.Behavior
            layoutBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean = mainViewModel.run {
                    !(isSearching.value || isSettingsDestination.value || isSelected.value)
                }
            })
        }
    }

    private fun ActivityMainBinding.bindSearchBar() = materialSearchBar.setOnClickListener {
        currentlyVisibleBaseFragment?.stopScrolling()
        navigateToSearchFragment()
    }

    private fun ActivityMainBinding.bindToolBar() {
        materialToolbar.apply {
            inflateMenu(R.menu.main_tool_bar)
            setOnSearchAction()
            setupNavAndMenu()
        }
    }

    private fun SimpleMaterialToolbar.setOnSearchAction() {
        searchActionView?.setQuery(mainViewModel.searchInput.value, true)
        searchActionView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("MainActivity", "Search text = $newText")
                mainViewModel.setSearchInput(newText)
                return true
            }
        })
    }

    private fun SimpleMaterialToolbar.setupNavAndMenu() {
        setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_to_favorites -> {
                    if (currentlyVisibleBaseFragment !is FavoritesFragment) mainViewModel.addToFavorites(
                        onSuccess = { numberOfItems ->
                            showToast(
                                getString(
                                    R.string.successfully_added_items_to_favorites, numberOfItems
                                )
                            )
                        },
                        onFailure = { message ->
                            showToast(
                                getString(
                                    R.string.unable_to_add_items_to_favorites, message
                                )
                            )
                        })
                    else mainViewModel.removeFromFavorites(onSuccess = { numberOfItems ->
                        showToast(
                            getString(
                                R.string.successfully_removed_items_from_favorites,
                                numberOfItems
                            )
                        )
                    }, onFailure = { message ->
                        showToast(
                            getString(
                                R.string.unable_to_remove_items_from_favorites, message
                            )
                        )
                    })
                }

                R.id.delete -> {
                    when (val visibleFragment = currentlyVisibleBaseFragment) {
                        is HomeFragment -> {
                            visibleFragment.uninstallSelectedApp()
                        }

                        is LocalFragment -> {
                            visibleFragment.deleteSelectedBackups()
                        }
                    }
                }

                R.id.select_all -> {
                    currentlyVisibleBaseFragment?.selectAllItems()
                }

                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
            true
        }
    }

    private fun ActivityMainBinding.bindNavigationBar() =
        navigationBar.setupNavigation(navController, onNavigate = { isReselected ->
            if (!isReselected) {
                floatingButton.setOnClickListener(null)
                currentlyVisibleBaseFragment?.stopScrolling()
            }
            animationFinished
        }, customNavigationOptions = { menuItem ->
            if (menuItem.itemId == R.id.settings) {
                setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
                setExitAnim(androidx.navigation.ui.R.anim.nav_default_exit_anim)
                setPopEnterAnim(R.animator.fragment_nav_enter_pop)
                setPopExitAnim(R.animator.fragment_nav_exit_pop)
            } else {
                setEnterAnim(R.animator.fragment_nav_enter)
                setExitAnim(R.animator.fragment_nav_exit)
                setPopEnterAnim(R.animator.fragment_nav_enter_pop)
                setPopExitAnim(R.animator.fragment_nav_exit_pop)
            }
        })

    private fun ActivityMainBinding.initObservers() {
        launchOnViewLifecycle {
            launch {
                repeatOnResumed {
                    observeNumberOfSelected()
                }
            }
            repeatOnCreated {
                mainViewModel.apply {
                    launch {
                        isSelected.collect { isSelected ->
                            mainActivityAnimator.animateOnSelection(isSelected, setSelectionMode)
                        }
                    }
                    launch {
                        isSearching.collect { isSearching ->
                            if (!isSearching) resetSearchInput()
                            mainActivityAnimator.animateOnSearch(isSearching)
                        }
                    }
                    isSettingsDestination.collect { isSettingsDestination ->
                        mainActivityAnimator.animateOnSettings(isSettingsDestination)
                    }
                }
            }
        }
    }

    private fun ActivityMainBinding.expandAppBarLayout(shouldExpand: Boolean) {
        appBarLayout.doOnPreDraw {
            if (shouldExpand)
                appBarLayout.setExpanded(true)
            else {
                val shouldMoveUp = currentlyVisibleBaseFragment?.shouldMoveFragmentUp()
                if (shouldMoveUp == true) {
                    Log.d("MainAnimator", "Collapsing the AppBarLayout")
                    appBarLayout.setExpanded(false)
                }
            }
        }
    }

    private fun registerReceivers() {
        registerReceiver(packageReceiver, intentFilter(
            ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED
        ) {
            addDataScheme("package")
        })
    }

    private fun setRootDialogs() {
        launchOnViewLifecycle {
            mainViewModel.onRootCheck(onRootNotGranted = {
                _rootAlertDialog = rootDialog(
                    title = getString(R.string.root_detected_title),
                    message = getString(R.string.not_granted_info)
                ).apply { show() }
            }, onDeviceNotRooted = {
                _rootAlertDialog = rootDialog(
                    title = getString(R.string.not_rooted_title),
                    message = getString(R.string.not_rooted_info)
                ).apply { show() }
            })
        }
    }

    fun MainRecyclerView.controlFloatingButton(onButtonClick: MainRecyclerView.() -> Unit) {
        binding.apply {
            if (floatingButton.hidePermanently) return
            controlAttachedButton(floatingButton)
            floatingButton.setOnClickListener {
                onButtonClick()
            }
        }
    }

    fun showConfigureFragment() {
        supportFragmentManager.apply {
            if (getVisibleFragment() is HomeViewPagerFragment && mainViewModel.selectionList.isNotEmpty()) {
                ConfigureSheetFragment().show(this, CONFIGURE_SHEET_TAG)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(
            MainPermission.NOTIFICATIONS.permissionName
        )
    }

    fun requestSignIn() =
        requestSignIn(resultLauncher = signInIntentLauncher, onAlreadySignedIn = {
            onSuccessfullySignedIn()
        })

    private fun onSuccessfullySignedIn() {
        launchOnViewLifecycle {
            launchProgressActivity(
                mainViewModel.selectionList.toTypedArray(), AppDataType.CLOUD
            )
            delay(250L)
            mainViewModel.setSelectionMode(false)
            val configureSheetFragment = getConfigureSheetFragment()
            configureSheetFragment?.dismiss()
        }
    }

    private fun getConfigureSheetFragment() =
        supportFragmentManager.findFragmentByTag(CONFIGURE_SHEET_TAG) as? ConfigureSheetFragment

    override fun onDestroy() {
        super.onDestroy()
        _rootAlertDialog?.dismiss()
        _rootAlertDialog = null
        unregisterReceivers(packageReceiver)
    }
}