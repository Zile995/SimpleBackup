package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.APP_DATA_TYPE_EXTRA
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

abstract class BaseActivity : AppCompatActivity(), BackPressHandler {

    private val ioDispatcher = Dispatchers.IO

    private var _permissionDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnBackPressedHandler()
        setMainBackgroundColor()
    }

    private fun addOnBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this) {
            onBackPress()
        }
    }

    private fun setMainBackgroundColor() =
        window.setBackgroundDrawableResource(R.color.main_background)

    override fun onBackPress() {
        val parentActivityIntent = NavUtils.getParentActivityIntent(this)
        parentActivityIntent?.let { upIntent ->
            if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot) {
                TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities()
            } else {
                // This activity is part of this app's task, so simply
                // navigate up to the logical parent activity.
                upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                NavUtils.navigateUpTo(this, upIntent)
            }
        } ?: finish()
    }

    fun startProgressActivity(selection: Array<String>?, appDataType: AppDataType?) {
        launchOnViewLifecycle {
            withContext(ioDispatcher) {
                Shell.getShell()
            }
            passBundleToActivity<ProgressActivity>(
                SELECTION_EXTRA to selection,
                APP_DATA_TYPE_EXTRA to appDataType
            )
        }
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

    fun showStoragePermissionDialog() {
        _permissionDialog = permissionDialog(
            title = getString(R.string.storage_permission),
            message = getString(R.string.storage_perm_info),
            onNegativeButtonPress = {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                    openPackageSettingsInfo(packageName)
                else
                    openManageFilesPermissionSettings()
            }).apply { show() }
    }


    fun showContactsPermissionDialog() {
        _permissionDialog = permissionDialog(
            title = getString(R.string.contacts_permission),
            message = getString(R.string.contacts_perm_info),
            onNegativeButtonPress = {
                openPackageSettingsInfo(packageName)
            }).apply { show() }
    }

    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).run {
            requestEmail()
            requestScopes(Scope(DriveScopes.DRIVE_FILE))
            build()
        }
        return GoogleSignIn.getClient(this, signInOptions)
    }

    @Suppress("DEPRECATION")
    fun requestSignIn() {
        val client = buildGoogleSignInClient()
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    fun handleSignInIntent(
        signInData: Intent,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        GoogleSignIn.getSignedInAccountFromIntent(signInData)
            .addOnSuccessListener { googleAccount ->
                // Get credential on success
                Log.d("BaseActivity", "Signed in as " + googleAccount.email)
                val credential = GoogleAccountCredential.usingOAuth2(
                    applicationContext, mutableListOf(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account

                // Get Google Drive Builder
                val googleDriveBuilder = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                // Google drive service
                googleDriveService = googleDriveBuilder.run {
                    applicationName = applicationContext.getString(R.string.app_name)
                    build()
                }
                onSuccess()
            }.addOnFailureListener { exception ->
                onFailure(exception.toString())
            }
    }

    private inline fun permissionDialog(
        title: String,
        message: String,
        crossinline onPositiveButtonPress: () -> Unit = {},
        crossinline onNegativeButtonPress: () -> Unit = {}
    ) = materialDialog(
        title = title,
        message = message,
        positiveButtonText = getString(R.string.ok),
        negativeButtonText = getString(R.string.set_manually),
        onPositiveButtonPress = onPositiveButtonPress,
        onNegativeButtonPress = onNegativeButtonPress,
        positiveColor = getColor(R.color.negative_dialog_text),
        negativeColor = getColor(R.color.positive_dialog_text)
    )

    protected fun rootDialog(title: String, message: String) = materialDialog(
        title = title,
        message = message,
        positiveButtonText = getString(R.string.ok),
        enableNegativeButton = false
    )

    override fun onDestroy() {
        super.onDestroy()
        _permissionDialog?.dismiss()
        _permissionDialog = null
    }

    companion object {
        var googleDriveService: Drive? = null
            private set

        const val REQUEST_CODE_SIGN_IN: Int = 400
    }
}

interface BackPressHandler {
    fun onBackPress()
}