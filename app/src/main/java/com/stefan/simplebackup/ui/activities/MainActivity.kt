package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupActionBarWithNavController
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.NotificationReceiver
import com.stefan.simplebackup.data.receivers.PackageReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.ui.views.SearchBarAnimator
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.root.RootChecker
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

    // Create SearchBarAnimator
    private val searchBarAnimator by lazy {
        SearchBarAnimator(WeakReference(this), WeakReference(binding))
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

        binding.apply {
            setNavController()
            bindViews()
            initObservers()
        }

        registerReceivers()
        setRootDialogs()
    }

    override fun onSupportNavigateUp(): Boolean {
        searchBarAnimator.revertSearchBarToInitialSize()
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
        searchBarAnimator.prepareWhenSearching(mainViewModel.isSearching.value)
    }

    private fun ActivityMainBinding.setNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.doesMatchDestination(R.id.search_action)) {
                searchBar.isEnabled = false
                mainViewModel.setSearching(true)
                floatingButton.hide()
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
        binSearchView()
        bindToolBar()
        bindSearchBar()
        bindBottomNavigationView()
    }

    private fun ActivityMainBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                mainViewModel.isSearching.collect { isSearching ->
                    if (isSearching) {
                        navigationBar.fadeOut(250L)
                    } else
                        navigationBar.fadeIn(250L)
                }
            }
        }
    }

    private fun ActivityMainBinding.binSearchView() {
        searchView.setTypeFace(getInterFontTypeFace())
    }

    private fun ActivityMainBinding.bindSearchBar() {
        hideSearchBarWhenSearching()
        searchBar.setOnClickListener {
            searchBarAnimator.animateSearchBarOnClick {
                navigateToSearchFragment()
            }
        }
    }

    private fun ActivityMainBinding.hideSearchBarWhenSearching() {
        root.doOnPreDraw {
            // Save the current dimensions before drawing the view.
            searchBar.saveTheCurrentDimensions()
            if (mainViewModel.isSearching.value) {
                // If we are searching, fill the parent (we need reverse animations later)
                searchBar.expandToParentView()
                // Also hide to avoid artifacts on split screen configuration change
                searchBar.hide()
                searchMagIcon.hide()
                searchText.hide()
                // And show the SearchView
                searchView.show()
            }
        }
    }

    private fun ActivityMainBinding.bindToolBar() {
        setSupportActionBar(materialToolbar)
        setupActionBarWithNavController(navController)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun ActivityMainBinding.bindBottomNavigationView() {
        navigationBar.navigateWithAnimation(navController, doBeforeNavigating = {
            floatingButton.setOnClickListener(null)
            return@navigateWithAnimation !mainViewModel.isSearching.value
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

    fun controlBottomView(shouldShow: Boolean) {
        binding.apply {
            if (!shouldShow) {
                navigationBar.hide()
            } else {
                navigationBar.show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers(packageReceiver, notificationReceiver)
    }
}


