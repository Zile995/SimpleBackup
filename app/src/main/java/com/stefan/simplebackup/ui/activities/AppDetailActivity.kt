package com.stefan.simplebackup.ui.activities

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
import com.stefan.simplebackup.databinding.ActivityDetailBinding
import com.stefan.simplebackup.utils.main.BitmapUtil
import com.stefan.simplebackup.utils.main.showToast
import com.stefan.simplebackup.utils.main.transformBytesToString
import com.stefan.simplebackup.viewmodels.AppDetailViewModel
import com.stefan.simplebackup.viewmodels.AppDetailViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

private const val TAG: String = "AppDetailActivity"
private const val REQUEST_CODE_SIGN_IN: Int = 400
private const val STORAGE_PERMISSION_CODE: Int = 500

class AppDetailActivity : AppCompatActivity() {

    // Package name reference
    private val myPackageName: String by lazy { applicationContext.packageName }

    private var _binding: ActivityDetailBinding? = null
    private val binding get() = _binding!!

    private val appDetailViewModel: AppDetailViewModel by viewModels {
        val selectedApp: AppData? = intent?.extras?.getParcelable("application")
        AppDetailViewModelFactory(selectedApp, application as MainApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        _binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            appDetailViewModel.selectedApp?.let {
                bindViews()
                setData()
            }
        }
    }

    private fun bindViews() {
        setToolBar()
        setCardViewSize()
        createMainCardView()
        createPackageDetails()
        createBackupButton()
        createBackupDriveButton()
        createDeleteButton()
    }

    private fun createBackupButton() {
        binding.backupButton.setOnClickListener {
            if (!checkPermission()) {
                requestPermission()
            } else {
                appDetailViewModel.apply {
                    createLocalBackup()
                }
            }
        }
    }

    private fun createMainCardView() {
        binding.backupCardView.setOnClickListener {
            appDetailViewModel.selectedApp?.let {
                openApplication(it.packageName)
            }
        }
    }

    private fun createPackageDetails() {
        binding.packageDetails.setOnClickListener {
            openPackageDetailsSettings()
        }
    }

    private fun setCardViewSize() {
        binding.apply {
            backupCardViewButtons.layoutParams
                .height = parentView.height -
                    toolbarBackup.height -
                    backupCardView.height -
                    backupCardViewPackage.height
        }
    }

    private fun createBackupDriveButton() {
        binding.backupDriveButton.setOnClickListener {
            requestSignIn()
        }
    }

    private fun createDeleteButton() {
        binding.floatingDeleteButton.setOnClickListener {
            lifecycleScope.launch {
                appDetailViewModel.selectedApp?.let { app ->
                    deleteApp(app.packageName)
                }
                delay(500)
                onBackPressed()
            }
        }
    }

    private fun setToolBar() {
        setSupportActionBar(binding.toolbarBackup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private suspend fun setData() {
        appDetailViewModel.selectedApp?.let { app ->
            binding.apply {
                setAppImage()
                textItemBackup.text = app.name
                chipPackageBackup.text = (app.packageName as CharSequence).toString()
                chipVersionBackup.text = (app.versionName as CharSequence).toString()
                chipDirBackup.text = (app.dataDir as CharSequence).toString()
                apkSize.text = app.apkSize.transformBytesToString()
                targetSdk.text = app.targetSdk.toString()
                minSdk.text = app.minSdk.toString()
                dataSize.text = app.dataSize.transformBytesToString()
            }
        }
    }

    private suspend fun setAppImage() {
        appDetailViewModel.selectedApp?.let { app ->
            BitmapUtil.setAppBitmap(app, applicationContext)
            Glide.with(applicationContext).apply {
                asBitmap()
                    .load(app.bitmap)
                    .placeholder(R.drawable.glide_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .dontAnimate()
                    .into(binding.applicationImageBackup)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_backup_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.force_stop -> {
                appDetailViewModel.selectedApp?.let { app ->
                    forceStop(app.packageName)
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun openApplication(packageName: String) {
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(packageName)
        intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN ->
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInIntent(data)
                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun deleteApp(packageName: String) {
        startActivity(Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${packageName}")
        })
    }

    private fun forceStop(packageName: String) {
        val activityManager =
            applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses(packageName)
        showToast("Application stopped!")
    }

    private fun openPackageDetailsSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = Uri.parse("package:" + appDetailViewModel.selectedApp?.packageName)
        })
    }

    private fun checkPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    lifecycleScope.launch {
                        appDetailViewModel.createLocalBackup()
                        Toast.makeText(
                            this@AppDetailActivity.applicationContext,
                            getString(R.string.storage_perm_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    permissionDialog()
                }
                return
            }
            else -> {
                // Ignore all other requests.
                return
            }
        }
    }

    private fun permissionDialog() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme).apply {
            setTitle(getString(R.string.storage_permission))
            setMessage(getString(R.string.storage_perm_info))
            setPositiveButton(getString(R.string.OK)) { dialog, _ ->
                dialog.cancel()
            }
                .setNegativeButton(getString(R.string.set_manually)) { _, _ ->
                    val uri = Uri.parse("package:$myPackageName")
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        data = uri
                    })
                }
        }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.positiveDialog))
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.positiveDialog))
        }
        alert.show()
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