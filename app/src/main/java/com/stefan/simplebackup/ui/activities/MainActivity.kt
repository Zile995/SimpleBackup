package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.NotificationReceiver
import com.stefan.simplebackup.data.receivers.PackageReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.utils.main.PreferenceHelper
import com.stefan.simplebackup.utils.root.RootChecker
import com.stefan.simplebackup.viewmodels.AppViewModel
import com.stefan.simplebackup.viewmodels.AppViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Create RootChecker Class instance and reference
    private val rootChecker by lazy { RootChecker(applicationContext) }
    private var isSearching = false

    // ViewModel
    private val appViewModel: AppViewModel by viewModels {
        AppViewModelFactory(application as MainApplication)
    }

    // NavController for fragments
    private lateinit var navController: NavController

    // Broadcast Receiver
    private val receiver: PackageReceiver by lazy {
        PackageReceiver(
            appViewModel,
            appViewModel.viewModelScope
        )
    }

    private val notificationReceiver: NotificationReceiver by lazy {
        NotificationReceiver()
    }

    // Binding properties
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // Flags
    private var isSubmitted: Boolean = false

    /**
     * - Standardna onCreate metoda Activity Lifecycle-a
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            launch {
                supportFragmentManager.setNavController()
                binding.bindViews()
            }
            registerBroadcast()
            setRootDialogs()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (binding.searchInput.hasFocus()) {
            isSearching = true
        }
        outState.putBoolean("isSearching", isSearching)
    }

    private fun FragmentManager.setNavController() {
        val navHostFragment = findFragmentById(R.id.nav_host_container) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun ActivityMainBinding.bindViews() {
        window.setBackgroundDrawableResource(R.color.background)
        bindBottomNavigationView()
    }

    private fun ActivityMainBinding.bindBottomNavigationView() {
        bottomNavigation.setupWithNavController(navController)
    }

    private fun registerBroadcast() {
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
        if (rootChecker.hasRootAccess()) {
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        isSubmitted = savedInstanceState.getBoolean("isSubmitted")
        isSearching = savedInstanceState.getBoolean("isSearching")
        if (isSearching) {
            binding.searchInput.requestFocus()
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        _binding = null
        unregisterReceiver(receiver)
        unregisterReceiver(notificationReceiver)
        super.onDestroy()
    }
}