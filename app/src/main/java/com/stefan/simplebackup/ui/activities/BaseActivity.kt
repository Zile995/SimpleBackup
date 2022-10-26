package com.stefan.simplebackup.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
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
import java.util.*

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
            onNegativeButtonPress = {
                openPackageSettingsInfo(packageName)
            })

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
                Log.d("GoogleServiceHandler", "Signed in as " + googleAccount.email)
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

    companion object {
        var googleDriveService: Drive? = null
            private set

        const val REQUEST_CODE_SIGN_IN: Int = 400
    }

}

interface BackPressHandler {
    fun onBackPress()
}