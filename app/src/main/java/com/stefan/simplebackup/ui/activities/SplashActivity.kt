package com.stefan.simplebackup.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.model.APP_DATA_TYPE_EXTRA
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.workers.WORK_REQUEST_TAG
import com.stefan.simplebackup.databinding.ActivitySplashBinding
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlin.properties.Delegates

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // Dispatchers
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // ViewBinding
    private val binding by viewBinding(ActivitySplashBinding::inflate)

    // WorkInfo
    private val workInfoFlow by lazy {
        WorkManager
            .getInstance(application)
            .getWorkInfosByTagLiveData(WORK_REQUEST_TAG)
            .asFlow()
    }

    private var permissionHolder: PermissionHolder by Delegates.observable(PermissionHolder()) { _, _, newGrantedStatus ->
        if (newGrantedStatus.isUsageStatsGranted && newGrantedStatus.isManageAllFilesGranted) {
            // Preheat the main root shell in the splash screen
            // so the app can use it afterwards without interrupting
            // application flow (e.g. root permission prompt)
            launchOnViewLifecycle {
                withContext(ioDispatcher) {
                    Shell.getShell()
                }
                // The main shell is now constructed and cached
                // Exit splash screen and enter main activity
                workInfoFlow.collect { workInfo ->
                    workInfo.firstOrNull()?.apply {
                        if (state == WorkInfo.State.RUNNING
                            || state == WorkInfo.State.ENQUEUED
                            || state == WorkInfo.State.SUCCEEDED
                        ) {
                            passBundleToActivity<ProgressActivity>(
                                SELECTION_EXTRA to null,
                                APP_DATA_TYPE_EXTRA to enumValues<AppDataType>()[PreferenceHelper.progressType],
                                customFlags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                        } else {
                            launchMainActivity()
                        }
                        finish()
                    } ?: launchMainActivity()
                }
            }
        }
        binding.updateViews(newGrantedStatus)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            root.changeBackgroundColor(applicationContext, R.color.bottom_view)
            bindStoragePermissionView()
            bindUsageStatsPermissionView()
        }
    }

    private fun ActivitySplashBinding.bindUsageStatsPermissionView() {
        usageStatsCard.setOnClickListener {
            openUsageAccessSettings()
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
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

    private fun ActivitySplashBinding.updateViews(newGrantedStatus: PermissionHolder) {
        usageStatsCard.isVisible = !newGrantedStatus.isUsageStatsGranted
        storagePermissionCard.isVisible = !newGrantedStatus.isManageAllFilesGranted
        applicationImage.isVisible =
            !(newGrantedStatus.isUsageStatsGranted && newGrantedStatus.isManageAllFilesGranted)
        welcomeLabel.isVisible =
            !(newGrantedStatus.isUsageStatsGranted && newGrantedStatus.isManageAllFilesGranted)
    }

    private data class PermissionHolder(
        var isUsageStatsGranted: Boolean = false,
        var isManageAllFilesGranted: Boolean = false
    )
}