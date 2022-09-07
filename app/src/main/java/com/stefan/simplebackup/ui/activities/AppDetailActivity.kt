package com.stefan.simplebackup.ui.activities

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
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
import java.util.*
import kotlin.math.abs


private const val TAG: String = "AppDetailActivity"
private const val REQUEST_CODE_SIGN_IN: Int = 400

class AppDetailActivity : BaseActivity() {
    private val binding by viewBinding(ActivityDetailBinding::inflate)

    private var isAnimating = false

    private val detailsViewModel: DetailsViewModel by viewModels {
        val selectedApp: AppData? = intent?.extras?.getParcelable(PARCELABLE_EXTRA)
        ViewModelFactory(application as MainApplication, selectedApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.statusBarColor = getColorFromResource(R.color.background)
        binding.apply {
            bindViews()
            setData()
        }
    }

    private fun ActivityDetailBinding.bindViews() {
        launchOnViewLifecycle {
            bindToolBar()
            bindAppBarLayout()
            bindCollapsingToolbarLayout()
        }
    }

    private fun ActivityDetailBinding.bindAppBarLayout() {
        var previousOffset = 0
        appBarDetailsLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val absoluteOffsetValue = abs(verticalOffset)
            val totalScrollRange = appBarLayout.totalScrollRange
            when (absoluteOffsetValue) {
                in 1 until totalScrollRange -> {
                    val offsetFactor = abs(verticalOffset) / totalScrollRange.toFloat()
                    val scaleFactor = 1f - offsetFactor * 0.7f
                    val alphaScaleFactor = 1f - offsetFactor
                    applicationImage.scaleX = scaleFactor
                    applicationImage.scaleY = scaleFactor
                    applicationImage.alpha = alphaScaleFactor
                }
                0 -> {
                    applicationImage.alpha = 1f
                    applicationImage.scaleX = 1f
                    applicationImage.scaleY = 1f
                    animateStatusBarColor(android.R.color.transparent)
                }
                totalScrollRange -> {
                    animateStatusBarColor(R.color.bottomView)
                }
            }
            if (absoluteOffsetValue < (totalScrollRange - (collapsingToolbar.scrimVisibleHeightTrigger - detailsToolbar.height))
                && absoluteOffsetValue < previousOffset
            ) {
                animateStatusBarColor(android.R.color.transparent)
            }
            previousOffset = absoluteOffsetValue
        }
    }

    private fun animateStatusBarColor(
        @ColorRes color: Int
    ) {
        if (window.statusBarColor == getColorFromResource(color) || isAnimating) return
        ObjectAnimator.ofObject(
            window,
            "statusBarColor",
            ArgbEvaluator(),
            window.statusBarColor,
            getColorFromResource(color)
        ).apply {
            duration = binding.collapsingToolbar.scrimAnimationDuration
            addUpdateListener {
                binding.detailsToolbar.setBackgroundColor(it.animatedValue as Int)
            }
            doOnStart {
                isAnimating = true
            }
            doOnEnd {
                isAnimating = false
            }
            start()
        }
    }

    private fun ActivityDetailBinding.bindToolBar() {
        setSupportActionBar(detailsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private suspend fun ActivityDetailBinding.bindCollapsingToolbarLayout() {
        detailsViewModel.selectedApp?.let { app ->
            val appImage = collapsingToolbar.findViewById<ImageView>(R.id.application_image)
            appImage.setOnClickListener {
                launchPackage(app.packageName)
            }
            app.setBitmap(applicationContext)
            appImage.loadBitmap(app.bitmap)
            collapsingToolbar.title = app.name
        }
    }

    private fun ActivityDetailBinding.setData() {
        detailsViewModel.selectedApp?.let { app ->
            appTypeChip.text =
                when {
                    app.isCloud -> resources.getString(R.string.cloud_backup)
                    app.isLocal -> resources.getString(R.string.local_backup)
                    else -> resources.getString(R.string.user_app)
                }
            packageNameLabel.text = app.packageName
            apkSizeLabel.text = getString(R.string.apk_size, app.apkSize.bytesToMegaBytesString())
            targetApiLabel.text = getString(R.string.target_sdk, app.targetSdk)
            minApiLabel.text = getString(R.string.min_sdk, app.minSdk)
        }
    }

    private fun ActivityDetailBinding.bindBackupButton() {
//        backupButton.setOnClickListener {
//            requestPermission(
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                requestPermissionLauncher,
//                continuationCallBack = {
//                    detailsViewModel.createLocalBackup()
//                },
//                dialogCallBack = {
//                    permissionDialog(
//                        title = getString(R.string.storage_permission),
//                        message = getString(R.string.storage_perm_info),
//                        positiveButtonText = getString(R.string.OK),
//                        negativeButtonText = getString(R.string.set_manually)
//                    )
//                })
//        }
    }

    private fun ActivityDetailBinding.bindBackupDriveButton() {
//        backupDriveButton.setOnClickListener {
//            requestSignIn()
//        }
    }

    private fun ActivityDetailBinding.bindDeleteButton() {
//        floatingDeleteButton.setOnClickListener {
//            lifecycleScope.launch {
//                detailsViewModel.selectedApp?.apply {
//                    onBackPressed()
//                    deletePackage(packageName)
//                }
//            }
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.details_tool_bar, menu)
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
            R.id.settings_info -> {
                detailsViewModel.selectedApp?.apply {
                    openPackageSettingsInfo(packageName)
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