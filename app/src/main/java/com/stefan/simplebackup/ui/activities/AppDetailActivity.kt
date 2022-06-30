package com.stefan.simplebackup.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.PARCELABLE_EXTRA
import com.stefan.simplebackup.databinding.ActivityDetailBinding
import com.stefan.simplebackup.ui.viewmodels.DetailsViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.launch
import java.util.*

private const val TAG: String = "AppDetailActivity"
private const val REQUEST_CODE_SIGN_IN: Int = 400

class AppDetailActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityDetailBinding::inflate)

    private val detailsViewModel: DetailsViewModel by viewModels {
        val selectedApp: AppData? = intent?.extras?.getParcelable(PARCELABLE_EXTRA)
        ViewModelFactory(application as MainApplication, selectedApp)
    }

    private val requestPermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                detailsViewModel.createLocalBackup()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        setContentView(binding.root)

        lifecycleScope.launch {
            detailsViewModel.selectedApp?.let {
                binding.apply {
                    bindViews()
                    setData()
                }
            }
        }
    }

    private fun ActivityDetailBinding.bindViews() {
        bindToolBar()
        bindCardViews()
        bindBackupButton()
        bindBackupDriveButton()
        bindDeleteButton()
    }

    private fun ActivityDetailBinding.bindToolBar() {
        setSupportActionBar(toolbarBackup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun ActivityDetailBinding.bindCardViews() {
        setCardViewSize()
        bindPackageDetails()
        backupCardView.setOnClickListener {
            detailsViewModel.selectedApp?.let { app ->
                launchPackage(app.packageName)
            }
        }
    }

    private fun ActivityDetailBinding.setCardViewSize() {
        backupCardViewButtons.layoutParams
            .height = parentView.height -
                toolbarBackup.height -
                backupCardView.height -
                backupCardViewPackage.height
    }

    private fun ActivityDetailBinding.bindBackupButton() {
        backupButton.setOnClickListener {
            requestPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                requestPermissionLauncher,
                continuationCallBack = {
                    detailsViewModel.createLocalBackup()
                },
                dialogCallBack = {
                    permissionDialog(
                        title = getString(R.string.storage_permission),
                        message = getString(R.string.storage_perm_info),
                        positiveButtonText = getString(R.string.OK),
                        negativeButtonText = getString(R.string.set_manually)
                    )
                })
        }
    }

    private fun ActivityDetailBinding.bindPackageDetails() {
        packageDetails.setOnClickListener {
            detailsViewModel.selectedApp?.apply {
                openPackageSettingsInfo(packageName)
            }
        }
    }

    private fun ActivityDetailBinding.bindBackupDriveButton() {
        backupDriveButton.setOnClickListener {
            requestSignIn()
        }
    }

    private fun ActivityDetailBinding.bindDeleteButton() {
        floatingDeleteButton.setOnClickListener {
            lifecycleScope.launch {
                detailsViewModel.selectedApp?.apply {
                    onBackPressed()
                    deletePackage(packageName)
                }
            }
        }
    }

    private suspend fun ActivityDetailBinding.setData() {
        detailsViewModel.selectedApp?.let { app ->
            app.setBitmap(applicationContext)
            applicationImageBackup.loadBitmap(app.bitmap)
            textItemBackup.text = app.name
            chipPackageBackup.text = (app.packageName as CharSequence).toString()
            chipVersionBackup.text = (app.versionName as CharSequence).toString()
            chipDirBackup.text = (app.dataDir as CharSequence).toString()
            textApkSize.text = app.apkSize.bytesToString()
            targetSdk.text = app.targetSdk.toString()
            minSdk.text = app.minSdk.toString()
            dataSize.text = app.dataSize.bytesToString()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_details_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.force_stop -> {
                detailsViewModel.selectedApp?.apply {
                    forceStopPackage(packageName)
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN ->
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInIntent(data)
                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
                requestEmail()
                requestScopes(Scope(DriveScopes.DRIVE_FILE))
            }.build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun handleSignInIntent(data: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener { googleAccount ->
                Log.d(TAG, "Signed in as " + googleAccount.email)
                val credential = GoogleAccountCredential.usingOAuth2(
                    this,
                    Collections.singleton(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("Simple Backup/1.0")
                    .build()
            }
            .addOnFailureListener { exception: Exception? ->
                Log.e(TAG, "Unable to sign in.", exception)
            }
    }
}