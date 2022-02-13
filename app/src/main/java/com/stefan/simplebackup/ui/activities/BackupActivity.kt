package com.stefan.simplebackup.ui.activities

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
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
import com.stefan.simplebackup.viewmodel.BackupViewModel
import com.stefan.simplebackup.viewmodel.BackupViewModelFactory
import com.topjohnwu.superuser.Shell
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
    private var thisPackageName: String = ""
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var toolBar: Toolbar
    private lateinit var textItem: MaterialTextView
    private lateinit var appImage: ImageView
    private lateinit var chipApkSize: Chip
    private lateinit var chipDataSize: Chip
    private lateinit var chipTargetSdk: Chip
    private lateinit var chipMinSdk: Chip
    private lateinit var backupButton: MaterialButton
    private lateinit var backupDriveButton: MaterialButton
    private lateinit var chipVersion: Chip
    private lateinit var chipPackage: Chip
    private lateinit var chipDir: Chip
    private lateinit var deleteButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cardView: MaterialCardView
    private lateinit var cardViewPackage: MaterialCardView
    private lateinit var cardViewButtons: MaterialCardView

    private val backupViewModel: BackupViewModel by viewModels {
        val application = application as DatabaseApplication
        val selectedApp: AppData? = intent?.extras?.getParcelable("application")
        BackupViewModelFactory(selectedApp, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        val binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        backupViewModel.selectedApp?.let {
            bindViews(binding)
            setCardViewSize()
            scope.launch {
                setData()
            }
        }
    }

    private suspend fun setData() {
        backupViewModel.selectedApp?.apply {
            setAppImage()
            textItem.text = getName()
            chipPackage.text = (getPackageName() as CharSequence).toString()
            chipVersion.text = (getVersionName() as CharSequence).toString()
            chipDir.text = (getDataDir() as CharSequence).toString()
            chipApkSize.text = FileUtil.transformBytesToString(getApkSize())
            chipTargetSdk.text = getTargetSdk().toString()
            chipMinSdk.text = getMinSdk().toString()
            dataPath = getDataDir()
            chipDataSize.text = getDataSize()
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
                    .into(appImage)
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

    private fun bindViews(binding: ActivityBackupBinding) {
        binding.apply {
            constraintLayout = parentView
            toolBar = toolbarBackup
            textItem = textItemBackup
            appImage = applicationImageBackup
            chipApkSize = apkSize
            chipDataSize = dataSize
            chipTargetSdk = targetSdk
            chipMinSdk = minSdk
            chipPackage = chipPackageBackup
            chipVersion = chipVersionBackup
            chipDir = chipDirBackup
            progressBar = backupProgressBar
            cardView = backupCardView
            cardViewPackage = backupCardViewPackage
            cardViewButtons = binding.backupCardViewButtons
            thisPackageName = this@BackupActivity.applicationContext.packageName
        }
        createBackupButton(binding)
        createBackupDriveButton(binding)
        createDeleteButton(binding)
        setToolBar()
    }

    private fun createBackupButton(binding: ActivityBackupBinding) {
        backupButton = binding.backupButton
        backupButton.setOnClickListener {
            if (!checkPermission()) {
                requestPermission()
            } else {
                backupViewModel.apply {
                    progressBar.visibility = View.VISIBLE
                    createLocalBackup()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun createBackupDriveButton(binding: ActivityBackupBinding) {
        backupDriveButton = binding.backupDriveButton
        backupDriveButton.setOnClickListener {
            requestSignIn()
        }
    }

    private fun createDeleteButton(binding: ActivityBackupBinding) {
        deleteButton = binding.floatingDeleteButton
        deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                onBackPressed()
                delay(150)
                launch {
                    backupViewModel.selectedApp?.getPackageName()
                        ?.let { packageName -> deleteApp(packageName) }
                }.join()
            }
        }
    }

    private fun setCardViewSize() {
        CoroutineScope(Dispatchers.Default).launch {
            val height =
                constraintLayout.height - toolBar.height - cardView.height - cardViewPackage.height
            cardViewButtons.layoutParams.height = height
        }
    }

    private fun setToolBar() {
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun deleteApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${packageName}")
        startActivity(intent)
    }

    private fun forceStop(packageName: String) {
        val activityManager =
            applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses(packageName)
        Toast.makeText(this@BackupActivity, "AppData stopped!", Toast.LENGTH_SHORT).show()
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
                        progressBar.visibility = View.VISIBLE
                        backupViewModel.createLocalBackup()
                        progressBar.visibility = View.GONE
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
                    val uri = Uri.parse("package:$thisPackageName")
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