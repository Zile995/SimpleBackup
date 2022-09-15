package com.stefan.simplebackup.ui.activities

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.stefan.simplebackup.BuildConfig
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivitySplashBinding
import com.stefan.simplebackup.utils.extensions.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        init {
            // Set settings before the main shell can be created
            val builder = Shell.Builder.create()
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                builder
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }

    private val binding by viewBinding(ActivitySplashBinding::inflate)

    private var permissionHolder: PermissionHolder by Delegates.observable(PermissionHolder()) { _, _, newGrantedStatus ->
        if (newGrantedStatus.isUsageStatsGranted && newGrantedStatus.isManageAllFilesGranted) {
            // Preheat the main root shell in the splash screen
            // so the app can use it afterwards without interrupting
            // application flow (e.g. root permission prompt)
            launchOnViewLifecycle {
                launch(ioDispatcher) {
                    Shell.getShell()
                }.join()
                // The main shell is now constructed and cached
                // Exit splash screen and enter main activity
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        binding.apply {
            binding.root.background =
                AppCompatResources.getDrawable(applicationContext, R.color.bottomView)
            usageStatsCard.isVisible = !newGrantedStatus.isUsageStatsGranted
            storagePermissionCard.isVisible = !newGrantedStatus.isManageAllFilesGranted
            applicationImage.isVisible =
                !(newGrantedStatus.isUsageStatsGranted && newGrantedStatus.isManageAllFilesGranted)
            welcomeLabel.isVisible =
                !(newGrantedStatus.isUsageStatsGranted && newGrantedStatus.isManageAllFilesGranted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            bindStoragePermissionView()
            bindUsageStatsPermissionView()
        }
    }

    private fun ActivitySplashBinding.bindUsageStatsPermissionView() {
        usageStatsCard.setOnClickListener {
            openUsageAccessSettings()
        }
    }

    private fun ActivitySplashBinding.bindStoragePermissionView() {
        storagePermissionCard.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                openMenageFilesPermissionSettings()
        }
    }

    override fun onStart() {
        super.onStart()
        checkForMainPermissions()
    }

    private val appOpsService by lazy { getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager }

    private fun checkForMainPermissions() {
        permissionHolder =
            PermissionHolder(
                isUsageStatsGranted = checkUsageStatsPermission(),
                isManageAllFilesGranted = checkManageAllFilesPermission()
            )
    }

    @Suppress("DEPRECATION")
    private fun checkUsageStatsPermission(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appOpsService.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        else
            appOpsService.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        return if (mode == AppOpsManager.MODE_DEFAULT)
            checkCallingOrSelfPermission(PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        else mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkManageAllFilesPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val mode = AppOpsManager.permissionToOp(MANAGE_EXTERNAL_STORAGE)?.let { appOpName ->
            appOpsService.unsafeCheckOpNoThrow(
                appOpName,
                Process.myUid(),
                packageName
            )
        }
        return if (mode == AppOpsManager.MODE_DEFAULT)
            checkCallingOrSelfPermission(MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else mode == AppOpsManager.MODE_ALLOWED
    }

    private data class PermissionHolder(
        var isUsageStatsGranted: Boolean = false,
        var isManageAllFilesGranted: Boolean = false
    )
}