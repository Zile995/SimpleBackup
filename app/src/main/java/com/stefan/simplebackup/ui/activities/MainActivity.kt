package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.NotificationReceiver
import com.stefan.simplebackup.data.receivers.PackageReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.root.RootChecker

class MainActivity : AppCompatActivity() {
    // Binding properties
    private val binding by viewBinding(ActivityMainBinding::inflate)

    // NavController for fragments
    private lateinit var navController: NavController

    // ViewModel
    private val mainViewModel: MainViewModel by viewModels {
        ViewModelFactory(application as MainApplication)
    }

    // Create RootChecker Class instance lazily
    private val rootChecker by lazy { RootChecker(applicationContext) }

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
        setContentView(binding.root)
        setNavController()
        binding.apply {
            bindViews()
            initObservers()
        }
        registerReceivers()
        setRootDialogs()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isSubmitted = savedInstanceState.getBoolean("isSubmitted")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("isSubmitted", isSubmitted)
    }

    private fun ActivityMainBinding.bindViews() {
        window.setBackgroundDrawableResource(R.color.background)
        bindSearchBar()
        bindBottomNavigationView()
    }

    private fun setNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.matchDestination(R.id.search_fragment)) {
                binding.toolBar.isClickable = false
                mainViewModel.changeSearching(true)
            } else {
                binding.toolBar.isClickable = true
                mainViewModel.changeSearching(false)
            }
        }
    }

    private fun navigateToSearchFragment() {
        navController.navigate(R.id.search_fragment, null, navOptions {
            anim {
                enter = R.animator.nav_default_enter_anim
                exit = R.animator.nav_default_exit_anim
                popEnter = R.animator.nav_default_pop_enter_anim
                popExit = R.animator.nav_default_pop_exit_anim
            }
        })
    }

    private fun ActivityMainBinding.bindSearchBar() {
        toolBar.setOnClickListener {
            saveCurrentToolbarValues()
            navigateToSearchFragment()
            toolBar.withRadiusAnimation(
                toolBar.height,
                (toolBar.parent as View).height,
                toolBar.width,
                (toolBar.parent as View).width
            )
            floatingButton.hide()
            floatingButton.setOnClickListener(null)
        }
    }


    private fun ActivityMainBinding.initObservers() {
        repeatOnViewLifecycleScope(Lifecycle.State.RESUMED) {
            mainViewModel.isSearching.collect { isSearching ->
                if (isSearching) {
                    expandToolBarToParentView()
                    bottomNavigationView.hide()
                } else {
                    bottomNavigationView.show()
                }
            }
        }
    }

    private fun ActivityMainBinding.expandToolBarToParentView() {
        toolBar.radius = 0f
        toolBar.layoutParams.height = (toolBar.parent as View).height
        toolBar.layoutParams.width = (toolBar.parent as View).width
        (toolBar.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin = 0
        (toolBar.layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin = 0
        toolBar.requestLayout()
    }

    private fun ActivityMainBinding.saveCurrentToolbarValues() {
        Log.d("ViewModel", "Saving height = ${toolBar.height}")
        Log.d("ViewModel", "Saving width = ${toolBar.width}")
        Log.d("ViewModel", "Saving radius = ${toolBar.radius}")
        Log.d("ViewModel", "Saving marginLeft = ${toolBar.marginLeft}")
        Log.d("ViewModel", "Saving marginRight = ${toolBar.marginRight}")
        mainViewModel.toolBarHeight = toolBar.height
        mainViewModel.toolBarWidth = toolBar.width
        mainViewModel.toolBarRadius = toolBar.radius
        mainViewModel.toolBarLeftMargin = toolBar.marginLeft
        mainViewModel.toolBarRightMargin = toolBar.marginRight
    }

    fun revertToolBarToInitialSize() {
        binding.apply {
            Log.d("ViewModel", "Saved height = ${mainViewModel.toolBarHeight}")
            Log.d("ViewModel", "Saved width = ${mainViewModel.toolBarWidth}")
            Log.d("ViewModel", "Saved radius = ${mainViewModel.toolBarRadius}")
            Log.d("ViewModel", "Saved marginLeft = ${mainViewModel.toolBarLeftMargin}")
            Log.d("ViewModel", "Saved marginRight = ${mainViewModel.toolBarRightMargin}")
            toolBar.withRadiusAnimation(
                toolBar.height,
                mainViewModel.toolBarHeight,
                toolBar.width,
                mainViewModel.toolBarWidth,
                savedCardViewRadius = mainViewModel.toolBarRadius,
            )
            (toolBar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin =
                mainViewModel.toolBarLeftMargin
            (toolBar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin =
                mainViewModel.toolBarRightMargin
            toolBar.requestLayout()
        }
    }

    private fun ActivityMainBinding.bindBottomNavigationView() {
        bottomNavigationView.navigateWithAnimation(navController, doBeforeNavigating = {
            floatingButton.setOnClickListener(null)
            !mainViewModel.isSearching.value
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
        onViewLifecycleScope {
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

    fun RecyclerView.controlFloatingButton() {
        binding.floatingButton.setOnClickListener {
            smoothSnapToPosition(0)
        }
        hideAttachedButton(binding.floatingButton)
    }

    fun requestFocus() {
        binding.apply {
            searchInput.requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                searchInput,
                InputMethodManager.SHOW_FORCED
            )
        }
    }

    fun controlBottomView(shouldShow: Boolean = false) {
        binding.apply {
            bottomNavigationView.withAnimation(shouldShow, bottomNavigationView.height.toFloat())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers(packageReceiver, notificationReceiver)
    }
}


