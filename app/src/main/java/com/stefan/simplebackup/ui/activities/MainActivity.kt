package com.stefan.simplebackup.ui.activities

import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
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
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.selectionFinished
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.AppBarLayoutStateChangedListener
import com.stefan.simplebackup.ui.views.MainActivityAnimator
import com.stefan.simplebackup.ui.views.MainActivityAnimator.Companion.animationFinished
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.root.RootChecker
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

    // Create RootChecker class instance lazily
    private val rootChecker by lazy { RootChecker(applicationContext) }

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
    private var isSubmitted = false

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
            shouldExit = false
            mainViewModel.setSelectionMode(false)
        }
        if (animationFinished && selectionFinished) {
            if (!shouldExit) {
                launchOnViewLifecycle {
                    shouldExit = true
                    showToast(R.string.press_back_again)
                    launch {
                        navController.currentDestination?.let { destination ->
                            if (destination.doesMatchDestination(R.id.home)) {
                                delay(1500L)
                                shouldExit = false
                            }
                        }
                    }
                }
            } else
                super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
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
            shouldExit = !destination.doesMatchDestination(R.id.home)
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
        bindSearchBar()
        bindToolBar()
        bindBottomNavigationView()
    }

    private fun ActivityMainBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                mainViewModel.apply {
                    launch {
                        isSearching.collect { isSearching ->
                            if (isSelected.value) return@collect
                            if (isSearching) {
                                floatingButton.setOnClickListener(null)
                                navigationBar.fadeOut(150L)
                                floatingButton.fadeOut(150L)
                                mainActivityAnimator.animateSearchBarOnClick()
                            } else {
                                navigationBar.fadeIn(150L)
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
                        expandAppBarLayout(isSelected)
                        if (isSelected) {
                            mainActivityAnimator.animateSearchBarOnSelection()
                        } else {
                            mainActivityAnimator.shrinkSearchBarToInitialSize()
                        }
                        floatingButton.changeOnSelection(isSelected)
                        materialToolbar.changeOnSelection(isSelected, setSelectionMode)
                        if (isSelected) {
                            navigationBar.moveVertically(
                                200,
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
                                200,
                                0f
                            ) {
                                val layoutParams =
                                    navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
                                layoutParams.bottomMargin = navigationBar.height
                                navHostContainer.layoutParams = layoutParams
                                navHostContainer.requestLayout()
                                getVisibleFragment()?.fixRecyclerViewScrollPosition()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun expandAppBarLayout(shouldExpand: Boolean) {
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
    }

    private fun getVisibleFragment(): BaseFragment<*>? {
        val viewPagerFragment = supportFragmentManager.getCurrentVisibleViewPagerFragment()
        return viewPagerFragment?.getVisibleFragment()
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

    private fun ActivityMainBinding.bindSearchBar() {
        materialSearchBar.setOnClickListener {
            navigateToSearchFragment()
        }
    }

    private fun ActivityMainBinding.bindToolBar() {
        materialToolbar.inflateMenu(R.menu.main_tool_bar)
        materialToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    //val searchView = menuItem?.actionView as MaterialSearchView
                }
                R.id.select_all -> {
                    val baseViewPagerFragment =
                        supportFragmentManager.getCurrentVisibleViewPagerFragment()
                    println("Current fragment = $baseViewPagerFragment")
                    baseViewPagerFragment?.selectAllItems()
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

    private fun ActivityMainBinding.bindBottomNavigationView() {
        navigationBar.navigateWithAnimation(navController, doBeforeNavigating = {
            floatingButton.setOnClickListener(null)
            return@navigateWithAnimation !(mainViewModel.isSearching.value
                    || mainViewModel.isSelected.value)
        })
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


