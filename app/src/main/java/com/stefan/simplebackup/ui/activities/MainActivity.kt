package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.broadcasts.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.broadcasts.NotificationBroadcastReceiver
import com.stefan.simplebackup.data.broadcasts.PackageBroadcastReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.fragments.AppListFragment
import com.stefan.simplebackup.ui.fragments.RestoreListFragment
import com.stefan.simplebackup.utils.main.PreferenceHelper
import com.stefan.simplebackup.utils.root.RootChecker
import com.stefan.simplebackup.viewmodels.AppViewModel
import com.stefan.simplebackup.viewmodels.AppViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Create RootChecker Class instance and reference
    private val rootChecker by lazy { RootChecker(applicationContext) }

    // Fragments
    private lateinit var activeFragment: Fragment
    private lateinit var homeFragment: Fragment
    private lateinit var restoreFragment: Fragment

    // ViewModel
    private val appViewModel: AppViewModel by viewModels {
        AppViewModelFactory(application as MainApplication)
    }

    // Broadcast Receiver
    private val receiver: PackageBroadcastReceiver by lazy {
        PackageBroadcastReceiver(
            appViewModel,
            appViewModel.viewModelScope
        )
    }

    private val notificationReceiver: NotificationBroadcastReceiver by lazy {
        NotificationBroadcastReceiver()
    }

    // Binding properties
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // Flags
    private var isSubmitted: Boolean = false
    private var doubleBackPressed: Boolean = false

    /**
     * - Standardna onCreate metoda Activity Lifecycle-a
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState.restoreSavedInstances()
        binding.bindViews()
        registerBroadcast()
    }

    private fun ActivityMainBinding.bindViews() {
        window.setBackgroundDrawableResource(R.color.background)
        bindBottomBar()
    }

    private fun Bundle?.restoreSavedInstances() {
        if (this == null) {
            setFragments()
        } else {
            restoreFragments()
        }
    }

    private fun ActivityMainBinding.bindBottomBar() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            val currentItem = bottomNavigation.selectedItemId

            menuItem.itemId.let { itemId ->
                when (itemId) {
                    R.id.homeFragment -> {
                        if (checkItem(currentItem, itemId)) {
                            showFragment(activeFragment, homeFragment)
                        }
                        activeFragment = homeFragment
                    }
                    R.id.restoreFragment -> {
                        if (checkItem(currentItem, itemId)) {
                            showFragment(activeFragment, restoreFragment)
                        }
                        activeFragment = restoreFragment
                    }
                }
            }
            doubleBackPressed = false
            true
        }
    }

    private fun setFragments() {
        homeFragment = AppListFragment()
        restoreFragment = RestoreListFragment()
        activeFragment = homeFragment
        supportFragmentManager.apply {
            commit {
                setReorderingAllowed(true)
                add(R.id.nav_host_fragment, homeFragment, "homeFragment")
                add(R.id.nav_host_fragment, restoreFragment, "restoreFragment")
                hide(restoreFragment)
            }
        }
    }

    private fun Bundle.restoreFragments() {
        supportFragmentManager.apply {
            getFragment(this@restoreFragments, "homeFragment")?.let {
                homeFragment = it
            }
            getFragment(this@restoreFragments, "restoreFragment")?.let {
                restoreFragment = it
            }
            getFragment(this@restoreFragments, "activeFragment")?.let {
                activeFragment = it
            }
        }
    }

    private fun showFragment(active: Fragment, new: Fragment) {
        supportFragmentManager.commit {
            hide(active)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            show(new)
        }
    }

    private fun checkItem(current: Int, selected: Int): Boolean {
        return current != selected
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
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isSubmitted", isSubmitted)
        supportFragmentManager.apply {
            putFragment(outState, "homeFragment", homeFragment)
            putFragment(outState, "restoreFragment", restoreFragment)
            putFragment(outState, "activeFragment", activeFragment)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        lifecycleScope.launch {
            setRootDialogs()
        }
        super.onResume()
    }

    override fun onBackPressed() {
        val homeItemId = R.id.homeFragment
        val selectedItemId = binding.bottomNavigation.selectedItemId
        if (selectedItemId == homeItemId && doubleBackPressed) {
            finish()
        } else {
            if (selectedItemId == homeItemId) {
                doubleBackPressed = true
                Toast.makeText(
                    applicationContext,
                    "Press back again to exit",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else
                binding.bottomNavigation.selectedItemId = homeItemId
        }
    }

    override fun onDestroy() {
        _binding = null
        unregisterReceiver(receiver)
        unregisterReceiver(notificationReceiver)
        super.onDestroy()
    }
}