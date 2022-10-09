package com.stefan.simplebackup.ui.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.APP_DATA_TYPE_EXTRA
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.openManageFilesPermissionSettings
import com.stefan.simplebackup.utils.extensions.openPackageSettingsInfo
import com.stefan.simplebackup.utils.extensions.passBundleToActivity
import com.stefan.simplebackup.utils.extensions.permissionDialog

abstract class BaseActivity : AppCompatActivity(), BackPressHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnBackPressedHandler()
        window.setBackgroundDrawableResource(R.color.main_background)
    }

    private fun addOnBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this) {
            onBackPress()
        }
    }

    override fun onBackPress() {
        finish()
    }

    fun startProgressActivity(selection: Array<String>, appDataType: AppDataType) {
        if (selection.isEmpty()) return
        passBundleToActivity<ProgressActivity>(
            SELECTION_EXTRA to selection,
            APP_DATA_TYPE_EXTRA to appDataType
        )
    }

    inline fun onMainPermissionRequest(
        mainPermission: MainPermission,
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionAlreadyGranted: () -> Unit = {},
        onPermissionRationale: () -> Unit
    ) {
        val appPermissionManager = AppPermissionManager(this)
        when {
            appPermissionManager.checkMainPermission(mainPermission) -> onPermissionAlreadyGranted()
            shouldShowRequestPermissionRationale(mainPermission.permissionName) -> onPermissionRationale()
            else -> permissionLauncher.launch(mainPermission.permissionName)
        }
    }

    inline fun requestStoragePermission(
        permissionLauncher: ActivityResultLauncher<String>,
        crossinline onPermissionAlreadyGranted: () -> Unit = {},
        onPermissionRationale: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            onMainPermissionRequest(
                mainPermission = MainPermission.STORAGE,
                permissionLauncher = permissionLauncher,
                onPermissionAlreadyGranted = { onPermissionAlreadyGranted() },
                onPermissionRationale = {
                    showStoragePermissionDialog()
                    onPermissionRationale()
                },
            )
        } else {
            proceedWithPermission(
                mainPermission = MainPermission.MANAGE_ALL_FILES,
                onPermissionGranted = {
                    onPermissionAlreadyGranted()
                },
                onPermissionDenied = {
                    showStoragePermissionDialog()
                }
            )
        }
    }

    inline fun requestContactsPermission(
        permissionLauncher: ActivityResultLauncher<String>,
        onPermissionAlreadyGranted: () -> Unit = {},
        onPermissionRationale: () -> Unit = {}
    ) = onMainPermissionRequest(
        mainPermission = MainPermission.CONTACTS,
        permissionLauncher = permissionLauncher,
        onPermissionAlreadyGranted = { onPermissionAlreadyGranted() },
        onPermissionRationale = {
            showContactsPermissionDialog()
            onPermissionRationale()
        }
    )

    inline fun proceedWithPermission(
        mainPermission: MainPermission,
        crossinline onPermissionGranted: () -> Unit = {},
        crossinline onPermissionDenied: () -> Unit = {}
    ) {
        val appPermissionManager = AppPermissionManager(this)
        if (appPermissionManager.checkMainPermission(mainPermission))
            onPermissionGranted()
        else
            onPermissionDenied()
    }

    fun showStoragePermissionDialog() =
        permissionDialog(
            title = getString(R.string.storage_permission),
            message = getString(R.string.storage_perm_info),
            positiveButtonText = getString(R.string.ok),
            negativeButtonText = getString(R.string.set_manually),
            onNegativeButtonPress = {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                    openPackageSettingsInfo(packageName)
                else
                    openManageFilesPermissionSettings()
            })


    fun showContactsPermissionDialog() =
        permissionDialog(
            title = getString(R.string.contacts_permission),
            message = getString(R.string.contacts_perm_info),
            positiveButtonText = getString(R.string.ok),
            negativeButtonText = getString(R.string.set_manually),
            onNegativeButtonPress = {
                openPackageSettingsInfo(packageName)
            })
}

interface BackPressHandler {
    fun onBackPress()
}