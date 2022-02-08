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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.broadcasts.PackageBroadcastReceiver
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.fragments.AppListFragment
import com.stefan.simplebackup.ui.fragments.RestoreListFragment
import com.stefan.simplebackup.utils.RootChecker
import com.stefan.simplebackup.viewmodel.AppViewModel
import com.stefan.simplebackup.viewmodel.AppViewModelFactory
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    // Create RootChecker Class instance and reference
    private var rootChecker = RootChecker(this)

    // Coroutine scope, on main thread
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    // UI
    private lateinit var bottomBar: BottomNavigationView

    private lateinit var activeFragment: Fragment
    private lateinit var homeFragment: Fragment
    private lateinit var restoreFragment: Fragment

    // ViewModel
    private val appViewModel: AppViewModel by viewModels {
        val mainApplication = application as DatabaseApplication
        AppViewModelFactory(mainApplication)
    }

    // Broadcast Receiver
    private val receiver: PackageBroadcastReceiver by lazy {
        PackageBroadcastReceiver(
            appViewModel,
            scope
        )
    }

    // Flags
    private var isSubmitted: Boolean = false
    private var doubleBackPressed: Boolean = false

    /**
     * - Standardna onCreate metoda Activity Lifecycle-a
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        // Activity ne treba da kreira fragment. Ne želimo da pri configuration change-u to radi
        if (savedInstanceState == null) {
            setFragments()
        } else {
            restoreFragments(savedInstanceState)
        }
        prepareActivity(savedInstanceState)
        registerBroadcast()
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

    private fun bindViews() {
        // Postavi View Binding
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createBottomBar(binding)
        println("Created bottomBar")
    }

    private fun createBottomBar(binding: ActivityMainBinding) {
        bottomBar = binding.bottomNavigation

        bottomBar.setOnItemSelectedListener { menuItem ->
            val currentItem = bottomBar.selectedItemId

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

    private fun checkItem(current: Int, selected: Int): Boolean {
        return current != selected
    }

    private fun prepareActivity(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // Pokupi sačuvano informaciju o tome da li je postavljen root upit.
            isSubmitted = savedInstanceState.getBoolean("isSubmitted")
        }
        this.window.setBackgroundDrawableResource(R.color.background)
        println("Prepared activity")
    }

    private fun registerBroadcast() {
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        })
    }

    private suspend fun setRootDialogs() {
        val rootSharedPref = getRootPreferences()
        if (!isSubmitted) {
            // Ostavićemo da Magisk prikazuje Toast kao obaveštenje da nemamo root access
            // Prikazuj kada se svaki put pozove onCreate metoda
            checkForRoot(rootSharedPref)
            isSubmitted = true
        }
        if (!rootSharedPref.getBoolean(
                "checked",
                false
            ) && rootChecker.isRooted() && !rootSharedPref.getBoolean("root_granted", false)
        ) {
            rootDialog(
                false,
                getString(R.string.root_detected),
                getString(R.string.not_granted)
            )
        } else if (!rootSharedPref.getBoolean(
                "checked",
                false
            ) && !rootChecker.isRooted()
        ) {
            rootDialog(
                false,
                getString(R.string.not_rooted),
                getString(R.string.not_rooted_info)
            )
        }
    }

    private fun checkForRoot(rootSharedPref: SharedPreferences) {
        if (rootChecker.hasRootAccess()) {
            rootSharedPref.edit().putBoolean("root_granted", true).apply()
        } else {
            rootSharedPref.edit().putBoolean("root_granted", false).apply()
        }
    }

    private fun getRootPreferences() =
        getSharedPreferences("root_access", MODE_PRIVATE)

    private fun rootDialog(checked: Boolean, title: String, message: String) {
        if (!checked) {
            val builder = AlertDialog.Builder(this, R.style.DialogTheme).apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton(getString(R.string.OK)) { dialog, _ ->
                    dialog.cancel()
                }
            }
            getRootPreferences().apply {
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
        scope.launch {
            setRootDialogs()
        }
        super.onResume()
    }

    override fun onBackPressed() {
        val homeItemId = R.id.appListFragment
        val selectedItemId = bottomBar.selectedItemId
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
                bottomBar.selectedItemId = homeItemId
        }
    }

    override fun onDestroy() {
        scope.cancel()
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}