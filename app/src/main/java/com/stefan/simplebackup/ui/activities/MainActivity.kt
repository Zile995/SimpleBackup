package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import com.stefan.simplebackup.R
import com.stefan.simplebackup.broadcasts.PackageBroadcastReceiver
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.broadcasts.ACTION_WORK_FINISHED
import com.stefan.simplebackup.broadcasts.NotificationBroadcastReceiver
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.fragments.AppListFragment
import com.stefan.simplebackup.ui.fragments.RestoreListFragment
import com.stefan.simplebackup.utils.root.RootChecker
import com.stefan.simplebackup.viewmodels.AppViewModel
import com.stefan.simplebackup.viewmodels.AppViewModelFactory
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    // Create RootChecker Class instance and reference
    private var rootChecker = RootChecker(this)

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    //Preferences
    private lateinit var rootSharedPref: SharedPreferences

    // Fragments
    private lateinit var activeFragment: Fragment
    private lateinit var homeFragment: Fragment
    private lateinit var restoreFragment: Fragment

    // ViewModel
    private val appViewModel: AppViewModel by viewModels {
        val mainApplication = application as MainApplication
        AppViewModelFactory(mainApplication)
    }

    // Broadcast Receiver
    private val receiver: PackageBroadcastReceiver by lazy {
        PackageBroadcastReceiver(
            appViewModel,
            lifecycleScope
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
        rootSharedPref = getSharedPreferences("root_access", MODE_PRIVATE)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bindViews(savedInstanceState)
        registerBroadcast()
    }

    private fun ActivityMainBinding.bindViews(savedInstanceState: Bundle?) {
        createBottomBar()
        if (savedInstanceState == null) {
            setFragments()
        } else {
            restoreFragments(savedInstanceState)
            isSubmitted = savedInstanceState.getBoolean("isSubmitted")
        }
        prepareActivity()
        println("Bind View finished")
    }

    private fun ActivityMainBinding.createBottomBar() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            val currentItem = bottomNavigation.selectedItemId

            menuItem.itemId.let { itemId ->
                when (itemId) {
                    R.id.appListFragment -> {
                        if (checkItem(currentItem, itemId)) {
                            showFragment(activeFragment, homeFragment)
                        }
                        activeFragment = homeFragment
                    }
                    R.id.restoreListFragment -> {
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

    private fun restoreFragments(savedInstanceState: Bundle) {
        supportFragmentManager.apply {
            getFragment(savedInstanceState, "homeFragment")?.let {
                homeFragment = it
            }
            getFragment(savedInstanceState, "restoreFragment")?.let {
                restoreFragment = it
            }
            getFragment(savedInstanceState, "activeFragment")?.let {
                activeFragment = it
            }
        }
    }

    private fun showFragment(active: Fragment, new: Fragment) {
        supportFragmentManager.commit {
            hide(active)
            setTransition(FragmentTransaction.TRANSIT_NONE)
            show(new)
        }
        println("Hiding $active and showing $new")
    }

    private fun checkItem(current: Int, selected: Int): Boolean {
        return current != selected
    }

    private fun prepareActivity() {
        window.setBackgroundDrawableResource(R.color.background)
        println("Prepared activity")
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
            checkForRoot()
            isSubmitted = true
        }
        if (!rootSharedPref.getBoolean(
                "checked",
                false
            ) && rootChecker.isRooted()
            && !rootSharedPref.getBoolean("root_granted", false)
        ) {
            rootDialog(
                getString(R.string.root_detected),
                getString(R.string.not_granted)
            )
        } else if (!rootSharedPref.getBoolean(
                "checked",
                false
            ) && !rootChecker.isRooted()
        ) {
            rootDialog(
                getString(R.string.not_rooted),
                getString(R.string.not_rooted_info)
            )
        }
    }

    private suspend fun checkForRoot() {
        withContext(ioDispatcher) {
            if (rootChecker.hasRootAccess()) {
                rootSharedPref.edit().putBoolean("root_granted", true).apply()
            } else {
                rootSharedPref.edit().putBoolean("root_granted", false).apply()
            }
        }
    }

    private fun rootDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(getString(R.string.OK)) { dialog, _ ->
                dialog.cancel()
            }
        }
        rootSharedPref.apply {
            edit()
                .putBoolean("checked", true)
                .apply()
        }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.positiveDialog))
        }
        alert.show()
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
        val homeItemId = R.id.appListFragment
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