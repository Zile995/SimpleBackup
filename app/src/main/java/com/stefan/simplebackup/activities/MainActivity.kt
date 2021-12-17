package com.stefan.simplebackup.activities

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.AppInfo
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.fragments.AppListFragment
import com.stefan.simplebackup.fragments.RestoreListFragment
import com.stefan.simplebackup.utils.PermissionUtils
import com.stefan.simplebackup.utils.RootChecker
import kotlinx.coroutines.*
import java.util.*

open class MainActivity : AppCompatActivity() {

    // Const values
    companion object {
        private const val STORAGE_PERMISSION_CODE: Int = 500
        lateinit var result: Deferred<MutableList<Application>>
    }

    // NavController
//    private lateinit var navController: NavController

    // Package name reference
    private var PACKAGE_NAME: String = ""

    // Create RootChecker Class instance and reference
    private var rootChecker = RootChecker(this)

    // Coroutine scope, on main thread
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    // UI
    private lateinit var bottomBar: BottomNavigationView

    // Flags
    private var isSubmitted: Boolean = false

    /**
     * - Standardna onCreate metoda Activity Lifecycle-a
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()

        // Activity ne treba da kreira fragment. Ne želimo da pri configuration change-u to radi
        if (savedInstanceState == null) {
            setFragment()
        }

        scope.launch {
            val rootSharedPref = getRootPreferences()
            launch {
                prepareActivity(savedInstanceState)
            }.join()
            launch {
                delay(250)
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
        }
    }

    private fun setFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.nav_host_fragment, AppListFragment())
        }
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

        bottomBar.setOnItemSelectedListener() {
            var selectedFragment: Fragment? = null

            when (it.itemId) {
                R.id.appListFragment -> {
                    selectedFragment = AppListFragment()
                }
                R.id.restoreListFragment -> {
                    selectedFragment = RestoreListFragment()
                }
            }
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment).commit()
            }
            true
        }
    }

    private fun prepareActivity(savedInstanceState: Bundle?) {
        setSavedInsanceState(savedInstanceState)
        setWindowBackground()
        setPackageName()
        println("Prepared activity")
    }

    private fun setPackageName() {
        PACKAGE_NAME = this.applicationContext.packageName
        println("Set packageName")
    }

    private fun setWindowBackground() {
        with(this) {
            window.setBackgroundDrawableResource(R.color.background)
        }
        println("Set windows background")
    }

    private fun getRootPreferences() =
        this@MainActivity.getSharedPreferences("root_access", MODE_PRIVATE)

    private fun setSavedInsanceState(savedInstanceState: Bundle?) {
        println("Set saved instance")
        if (savedInstanceState != null) {
            // Pokupi sačuvano informaciju o tome da li je postavljen root upit.
            isSubmitted = savedInstanceState.getBoolean("isSubmitted");
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isSubmitted", isSubmitted)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        if (!checkPermission()) {
            requestPermission()
        }
        super.onResume()
    }

    private fun checkForRoot(rootSharedPref: SharedPreferences) {
        if (rootChecker.hasRootAccess()) {
            rootSharedPref.edit().putBoolean("root_granted", true).apply()
        } else {
            rootSharedPref.edit().putBoolean("root_granted", false).apply()
        }
    }

    private fun checkPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (PermissionUtils.neverAskAgainSelected(
                this,
                WRITE_EXTERNAL_STORAGE
            )
        ) {
            permissionDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.size > 0) {
                    val WRITE_EXTERNAL_STORAGE =
                        grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            getString(R.string.storage_perm_success),
                            Toast.LENGTH_LONG
                        ).show();
                    } else {
                        PermissionUtils.setShowDialog(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }
                }
            }
            else -> {
                throw Exception("Wrong request code")
            }
        }
    }

    private fun permissionDialog() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.storage_permission))
            .setMessage(getString(R.string.storage_perm_info))
            .setPositiveButton(getString(R.string.set_manually)) { _, _ ->
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = Uri.parse("package:" + PACKAGE_NAME)
                intent.setData(uri)
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                Process.killProcess(Process.myPid())
            }
            .setCancelable(false)
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.red))
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.blue))
        }
        alert.show()
    }

    private fun rootDialog(checked: Boolean, title: String, message: String) {
        if (!checked) {
            val builder = AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.OK)) { dialog, _ ->
                    dialog.cancel()
                }
            this.getSharedPreferences("root_access", MODE_PRIVATE).edit()
                .putBoolean("checked", true).apply()
            val alert = builder.create()
            alert.setOnShowListener {
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(resources.getColor(R.color.blue))
            }
            alert.show()
        }
    }
}