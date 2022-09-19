package com.stefan.simplebackup.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.stefan.simplebackup.BuildConfig
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
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
        permissionHolder
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
                openManageFilesPermissionSettings()
        }
    }

    override fun onStart() {
        super.onStart()
        checkForMainPermissions()
    }

    private fun checkForMainPermissions() {
        val appPermissionManager = AppPermissionManager(applicationContext)
        appPermissionManager.apply {
            permissionHolder =
                PermissionHolder(
                    isUsageStatsGranted = checkUsageStatsPermission(),
                    isManageAllFilesGranted = checkManageAllFilesPermission()
                )
        }
    }

    private data class PermissionHolder(
        var isUsageStatsGranted: Boolean = false,
        var isManageAllFilesGranted: Boolean = false
    )
}