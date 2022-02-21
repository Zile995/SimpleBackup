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
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.databinding.ActivityBackupBinding
import com.stefan.simplebackup.utils.FileUtil
import com.stefan.simplebackup.viewmodels.BackupViewModel
import com.stefan.simplebackup.viewmodels.BackupViewModelFactory
import kotlinx.coroutines.*
import java.util.*

class BackupActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = "BackupActivity"
        private const val REQUEST_CODE_SIGN_IN: Int = 400
        private const val STORAGE_PERMISSION_CODE: Int = 500
        private lateinit var dataPath: String
    }

    // Package name reference
    private val myPackageName: String by lazy { applicationContext.packageName }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _binding: ActivityBackupBinding? = null
    private val binding get() = _binding!!

    private val backupViewModel: BackupViewModel by viewModels {
        val application = application as DatabaseApplication
        val selectedApp: AppData? = intent?.extras?.getParcelable("application")
        BackupViewModelFactory(selectedApp, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        _binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        backupViewModel.selectedApp?.let {
            bindViews()
            scope.launch {
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
                backupViewModel.apply {
                    createLocalBackup()
                }
            }
        }
    }

    private fun createMainCardView() {
        binding.backupCardView.setOnClickListener {
            backupViewModel.selectedApp?.let {
                openApplication(it.getPackageName())
            }
        }
    }

    private fun createPackageDetails() {
        binding.packageDetails.setOnClickListener {
            openPackageDetailsSettings()
        }
    }

    private fun setCardViewSize() {
        val constraintLayout = binding.parentView
        val toolBar = binding.toolbarBackup
        val cardView = binding.backupCardView
        val cardViewPackage = binding.backupCardViewPackage
        val cardViewButtons = binding.backupCardViewButtons
        val height =
            constraintLayout.height - toolBar.height - cardView.height - cardViewPackage.height
        cardViewButtons.layoutParams.height = height
    }

    private fun createBackupDriveButton() {
        binding.backupDriveButton.setOnClickListener {
            requestSignIn()
        }
    }

    private fun createDeleteButton() {
        binding.floatingDeleteButton.setOnClickListener {
            scope.launch {
                backupViewModel.selectedApp?.let { app ->
                    deleteApp(app.getPackageName())
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
        binding.apply {
            backupViewModel.selectedApp?.apply {
                setAppImage()
                textItemBackup.text = getName()
                chipPackageBackup.text = (getPackageName() as CharSequence).toString()
                chipVersionBackup.text = (getVersionName() as CharSequence).toString()
                chipDirBackup.text = (getDataDir() as CharSequence).toString()
                apkSize.text = FileUtil.transformBytesToString(getApkSize())
                targetSdk.text = getTargetSdk().toString()
                minSdk.text = getMinSdk().toString()
                dataPath = getDataDir()
                dataSize.text = FileUtil.transformBytesToString(getDataSize())
            }
        }
    }

    private suspend fun setAppImage() {
        backupViewModel.selectedApp?.let { app ->
            FileUtil.setAppBitmap(app, applicationContext)
            Glide.with(applicationContext).apply {
                asBitmap()
                    .load(app.getBitmap())
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
                backupViewModel.selectedApp?.let { app ->
                    forceStop(app.getPackageName())
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
        super.onDestroy()
        scope.cancel()
        _binding = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        scope.cancel()
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
        Toast.makeText(applicationContext, "AppData stopped!", Toast.LENGTH_SHORT).show()
    }

    private fun openPackageDetailsSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = Uri.parse("package:" + backupViewModel.selectedApp?.getPackageName())
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
                    scope.launch {
                        backupViewModel.createLocalBackup()
                        Toast.makeText(
                            this@BackupActivity.applicationContext,
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
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
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