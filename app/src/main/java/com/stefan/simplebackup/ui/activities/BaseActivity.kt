package com.stefan.simplebackup.ui.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermissions
import com.stefan.simplebackup.utils.extensions.openManageFilesPermissionSettings
import com.stefan.simplebackup.utils.extensions.openPackageSettingsInfo
import com.stefan.simplebackup.utils.extensions.permissionDialog

abstract class BaseActivity : AppCompatActivity(), BackPressHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnBackPressedHandler()
        window.setBackgroundDrawableResource(R.color.background)
    }

    private fun addOnBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this) {
            onBackPress()
        }
    }

    override fun onBackPress() {
        finish()
    }

    inline fun onMainPermissionCheck(
        mainPermission: MainPermissions,
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionRationale: () -> Unit
    ) {
        when {
            shouldShowRequestPermissionRationale(mainPermission.permissionString) -> onPermissionRationale()
            else -> permissionLauncher.launch(mainPermission.permissionString)
        }
    }

    inline fun requestStoragePermission(
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionRationale: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            onMainPermissionCheck(
                mainPermission = MainPermissions.STORAGE,
                permissionLauncher = permissionLauncher,
                onPermissionRationale = {
                    permissionDialog(
                        title = getString(R.string.storage_permission),
                        message = getString(R.string.storage_perm_info),
                        positiveButtonText = getString(R.string.ok),
                        negativeButtonText = getString(R.string.set_manually),
                        onNegativeButtonPress = {
                            openPackageSettingsInfo(packageName)
                        })
                    onPermissionRationale()
                },
            )
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            val appPermissionManager = AppPermissionManager(applicationContext)
            val isManageAllFilesGranted = appPermissionManager.checkManageAllFilesPermission()
            if (!isManageAllFilesGranted) {
                permissionDialog(
                    title = getString(R.string.storage_permission),
                    message = getString(R.string.storage_perm_info),
                    positiveButtonText = getString(R.string.ok),
                    negativeButtonText = getString(R.string.set_manually),
                    onNegativeButtonPress = {
                        openManageFilesPermissionSettings()
                    }
                )
            }
        }
    }

    inline fun requestContactsPermission(
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionRationale: () -> Unit = {}
    ) = onMainPermissionCheck(
        mainPermission = MainPermissions.CONTACTS,
        permissionLauncher = permissionLauncher,
        onPermissionRationale = {
            permissionDialog(
                title = getString(R.string.contacts_permission),
                message = getString(R.string.contacts_perm_info),
                positiveButtonText = getString(R.string.ok),
                negativeButtonText = getString(R.string.set_manually),
                onNegativeButtonPress = {
                    openPackageSettingsInfo(packageName)
                })
            onPermissionRationale()
        }
    )
}

interface BackPressHandler {
    fun onBackPress()
}