package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.content.IntentFilter
import android.opengl.Visibility
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.NotificationReceiver
import com.stefan.simplebackup.data.receivers.PackageReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.hideAttachedButton
import com.stefan.simplebackup.utils.root.RootChecker
import com.stefan.simplebackup.viewmodels.HomeViewModel
import com.stefan.simplebackup.viewmodels.HomeViewModelFactory
import kotlinx.coroutines.launch

typealias FloatingButtonCallback = (Boolean) -> Unit

class MainActivity : AppCompatActivity() {
    // Binding properties
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // NavController for fragments
    private lateinit var navController: NavController

    // ViewModel
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(application as MainApplication)
    }

    // Create RootChecker Class instance and reference
    private val rootChecker by lazy { RootChecker(this) }

    // Broadcast receivers
    private val receiver: PackageReceiver by lazy {
        PackageReceiver(
            homeViewModel,
            homeViewModel.viewModelScope
        )
    }
    private val notificationReceiver: NotificationReceiver by lazy {
        NotificationReceiver()
    }

    // Flags
    private var isSubmitted = false
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            launch {
                setNavController()
                binding.apply {
                    bindViews()
                    initObservers()
                }
            }
            registerBroadcasts()
            setRootDialogs()
        }
    }

    fun RecyclerView.controlFloatingButton(isButtonVisible: Boolean) {
        binding.floatingButton.setOnClickListener {
            this.smoothScrollToPosition(0)
        }
        hideAttachedButton(isButtonVisible) { isVisible ->
            homeViewModel.changeButtonVisibility(isVisible)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isSubmitted = savedInstanceState.getBoolean("isSubmitted")
        isSearching = savedInstanceState.getBoolean("isSearching")
        if (isSearching) {
            binding.searchInput.requestFocus()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (binding.searchInput.hasFocus()) {
            isSearching = true
        }
        outState.putBoolean("isSearching", isSearching)
    }

    private fun setNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun ActivityMainBinding.bindViews() {
        window.setBackgroundDrawableResource(R.color.background)
        bindBottomNavigationView()
    }

    private fun ActivityMainBinding.initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                homeViewModel.button.collect { isVisible ->
                    if (isVisible) floatingButton.show() else floatingButton.hide()
                }
            }
        }
    }

    private fun ActivityMainBinding.bindBottomNavigationView() {
        // TODO: Should change animations, and save button states on fragment change
        bottomNavigation.setupWithNavController(navController)
    }

    private fun registerBroadcasts() {
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        })
        registerReceiver(notificationReceiver, IntentFilter().apply {
            addAction(ACTION_WORK_FINISHED)
        })
    }

    private suspend fun setRootDialogs() {
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

    override fun onDestroy() {
        _binding = null
        unregisterReceiver(receiver)
        unregisterReceiver(notificationReceiver)
        super.onDestroy()
    }
}


