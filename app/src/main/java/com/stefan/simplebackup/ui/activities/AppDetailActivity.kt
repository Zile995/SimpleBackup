package com.stefan.simplebackup.ui.activities

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.appcompat.content.res.AppCompatResources
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
import com.stefan.simplebackup.utils.file.BitmapUtil.toByteArray
import java.util.*
import kotlin.math.abs

private const val TAG: String = "AppDetailActivity"
private const val REQUEST_CODE_SIGN_IN: Int = 400

class AppDetailActivity : BaseActivity() {
    private val binding by viewBinding(ActivityDetailBinding::inflate)

    private var isToolbarAnimating = false

    private val detailsViewModel: DetailsViewModel by viewModels {
        val selectedApp = intent?.extras?.parcelable<AppData>(PARCELABLE_EXTRA)
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
                    moveCardView(totalScrollRange)
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
                moveCardView(0)
            }
            previousOffset = absoluteOffsetValue
        }
    }

    private fun animateStatusBarColor(
        @ColorRes color: Int
    ) {
        if (window.statusBarColor == getColorFromResource(color) || isToolbarAnimating) return
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
                isToolbarAnimating = true
            }
            doOnEnd {
                isToolbarAnimating = false
            }
            start()
        }
    }

    private fun ActivityDetailBinding.moveCardView(translation: Int) {
        if (mainActions.translationY == translation.toFloat() || isToolbarAnimating) return
        ObjectAnimator.ofFloat(mainActions, "translationY", translation.toFloat()).apply {
            duration = 100L
            start()
        }
    }

    private fun ActivityDetailBinding.bindToolBar() {
        setSupportActionBar(detailsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private suspend fun ActivityDetailBinding.bindCollapsingToolbarLayout() {
        detailsViewModel.app?.let { app ->
            val appImage = collapsingToolbar.findViewById<ImageView>(R.id.application_image)
            appImage.setOnClickListener {
                launchPackage(app.packageName)
            }
            app.setBitmap(applicationContext, onFailure = {
                getResourceDrawable(R.drawable.ic_error)?.toByteArray()
                    ?: byteArrayOf()
            })
            appImage.loadBitmap(app.bitmap)
            collapsingToolbar.title = app.name
        }
    }

    private fun ActivityDetailBinding.setData() {
        detailsViewModel.app?.let { app ->
            appTypeChip.text =
                when {
                    app.isCloud -> resources.getString(R.string.cloud_backup)
                    app.isLocal -> resources.getString(R.string.local_backup)
                    else -> resources.getString(R.string.user_app)
                }
            @SuppressLint("SetTextI18n")
            versionNameLabel.text = "v${app.versionName}"
            installedDateLabel.text = when {
                app.isCloud || app.isLocal -> getString(R.string.backed_up_on, app.getDateString())
                else -> getString(R.string.first_installed_on, app.getDateString())
            }
            isSplitChip.isVisible = app.isSplit
            isSplitChip.text = getString(R.string.split)
            packageNameLabel.text = app.packageName
            apkSizeLabel.text = getString(R.string.apk_size, app.apkSize.bytesToMegaBytesString())
            targetApiLabel.text = getString(R.string.target_sdk, app.targetSdk)
            minApiLabel.text = getString(R.string.min_sdk, app.minSdk)
            deleteAppButton.setOnClickListener {
                detailsViewModel.app?.apply {
                    deletePackage(packageName)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.details_tool_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        detailsViewModel.app?.apply {
            menu?.findItem(R.id.add_to_favorites)?.apply {
                icon =
                    if (favorite) AppCompatResources.getDrawable(
                        applicationContext,
                        R.drawable.ic_favorite
                    )
                    else
                        AppCompatResources.getDrawable(
                            applicationContext,
                            R.drawable.ic_unstarred
                        )
                tooltipText = if (favorite)
                    getString(R.string.remove_from_favorites)
                else
                    getString(R.string.add_to_favorites)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.force_stop -> {
                detailsViewModel.app?.apply {
                    forceStopPackage(packageName)
                }
                true
            }
            R.id.settings_info -> {
                detailsViewModel.app?.apply {
                    openPackageSettingsInfo(packageName)
                }
                true
            }
            R.id.add_to_favorites -> {
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPress()
        return true
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN ->
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInIntent(data)
                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Suppress("DEPRECATION")
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