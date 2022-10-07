package com.stefan.simplebackup.ui.activities

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.PARCELABLE_EXTRA
import com.stefan.simplebackup.databinding.ActivityDetailBinding
import com.stefan.simplebackup.ui.viewmodels.DetailsViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.file.BitmapUtil.toByteArray
import com.stefan.simplebackup.utils.file.FileUtil
import kotlinx.coroutines.launch
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

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                //
            } else {
                showStoragePermissionDialog()
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocalWork()
            } else {
                showStoragePermissionDialog()
            }
        }

    private val packageReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                detailsViewModel.viewModelScope.launch {
                    if ((intent.action == Intent.ACTION_PACKAGE_ADDED || (intent.action == Intent.ACTION_PACKAGE_REMOVED && intent.extras?.getBoolean(
                            Intent.EXTRA_REPLACING
                        ) == false)) && intent.data?.encodedSchemeSpecificPart == detailsViewModel.app?.packageName
                    ) {
                        onBackPress()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.statusBarColor = getColorFromResource(R.color.background)
        binding.apply {
            bindViews()
            initObservers()
        }
        registerPackageReceiver()
    }

    private fun registerPackageReceiver() {
        registerReceiver(packageReceiver, intentFilter(
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED
        ) {
            addDataScheme("package")
        })
    }

    private fun ActivityDetailBinding.initObservers() {
        launchOnViewLifecycle {
            detailsViewModel.observeLocalBackup(
                onBackupFileChanged = {
                    onBackPress()
                }
            )
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                detailsViewModel.archNames.collect { archNames ->
                    Log.d("ChipGroup", "Arch names = $archNames")
                    archNames?.let {
                        architectureChipGroup.addArchChipsToChipGroup(archNames)
                    }
                }
            }
        }
    }

    private fun ActivityDetailBinding.bindViews() {
        launchOnViewLifecycle {
            bindToolBar()
            bindAppBarLayout()
            bindDeleteButton()
            bindCloudBackupButton()
            bindLocalBackupButton()
            setData(detailsViewModel.app)
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
                    mainActions.translationY = absoluteOffsetValue.toFloat()
                }
                0 -> {
                    applicationImage.alpha = 1f
                    applicationImage.scaleX = 1f
                    applicationImage.scaleY = 1f
                    mainActions.translationY = 0f
                    animateStatusBarColor(android.R.color.transparent)
                }
                totalScrollRange -> {
                    animateStatusBarColor(R.color.bottomView)
                    mainActions.translationY = totalScrollRange.toFloat()
                }
            }
            if (absoluteOffsetValue < (totalScrollRange - (collapsingToolbar.scrimVisibleHeightTrigger - detailsToolbar.height)) && absoluteOffsetValue < previousOffset) {
                animateStatusBarColor(android.R.color.transparent)
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

    private fun ActivityDetailBinding.bindToolBar() {
        detailsToolbar.apply {
            inflateMenu(R.menu.details_tool_bar)
            menu.setFavoriteIcon()
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
            setNavigationOnClickListener { onBackPress() }
            setOnMenuItemClickListener { menuItem ->
                detailsViewModel.app?.run {
                    when (menuItem.itemId) {
                        R.id.force_stop -> {
                            forceStopPackage(packageName)
                        }
                        R.id.settings_info -> {
                            openPackageSettingsInfo(packageName)
                        }
                        R.id.add_to_favorites -> {
                            detailsViewModel.changeFavorites(
                                onSuccess = { isFavorite ->
                                    menu?.setFavoriteIcon()
                                    if (isFavorite)
                                        showToast(R.string.added_to_favorites)
                                    else
                                        showToast(R.string.removed_from_favorites)
                                },
                                onFailure = { message ->
                                    showToast(
                                        getString(
                                            R.string.unable_to_change_favorites,
                                            message
                                        )
                                    )
                                }
                            )
                        }
                    }
                    true
                } ?: false
            }
        }
    }

    private fun ActivityDetailBinding.bindLocalBackupButton() {
        detailsViewModel.app?.apply {
            if (isLocal) {
                localBackupButton.setImageResource(R.drawable.ic_restore)
                localBackupButton.tooltipText = getString(R.string.restore)
            }
        }
        localBackupButton.setOnClickListener {
            requestStoragePermission(storagePermissionLauncher, onPermissionAlreadyGranted = {
                startLocalWork()
            })
        }
    }

    private fun ActivityDetailBinding.bindCloudBackupButton() {
        detailsViewModel.app?.apply {
            cloudBackupButton.isVisible = !isLocal
        }
        cloudBackupButton.setOnClickListener {
            proceedWithPermission(MainPermission.MANAGE_ALL_FILES, onPermissionGranted = {
                requestContactsPermission(contactsPermissionLauncher, onPermissionAlreadyGranted = {
                    //
                })
            }, onPermissionDenied = {
                showStoragePermissionDialog()
            })
        }
    }

    private fun ActivityDetailBinding.bindDeleteButton() {
        detailsViewModel.app?.apply {
            if (isLocal) deleteButton.tooltipText = getString(R.string.delete_backup)
        }
        deleteButton.setOnClickListener {
            detailsViewModel.app?.apply {
                if (isLocal) detailsViewModel.deleteLocalBackup(onSuccess = {
                    showToast(getString(R.string.backup_deleted_successfully, name))
                }, onFailure = { message ->
                    showToast(
                        getString(
                            R.string.backup_deleted_successfully, "$name $message"
                        )
                    )
                }).invokeOnCompletion {
                    onBackPress()
                }
                else uninstallPackage(packageName)
            }
        }
    }

    private suspend fun ActivityDetailBinding.setCollapsingToolbarData(app: AppData) {
        app.apply {
            val appImage = collapsingToolbar.findViewById<ImageView>(R.id.application_image)
            appImage.setOnClickListener {
                if (isLocal)
                    openFilePath("${FileUtil.localDirPath}/$packageName")
                else
                    launchPackage(packageName)
            }
            setBitmapFromPrivateFolder(context = this@AppDetailActivity, onFailure = {
                getResourceDrawable(R.drawable.ic_error)?.toByteArray() ?: byteArrayOf()
            })
            appImage.loadBitmap(bitmap)
            collapsingToolbar.title = name
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun ActivityDetailBinding.setData(app: AppData?) {
        app?.apply {
            setCollapsingToolbarData(this)
            appTypeChip.text = when {
                isCloud && isLocal -> resources.getString(R.string.cloud_backup)
                isLocal -> resources.getString(R.string.local_backup)
                else -> resources.getString(R.string.user_app)
            }
            installedDateLabel.text = when {
                isCloud || isLocal -> getString(R.string.backed_up_on, app.getDateString())
                else -> getString(R.string.first_installed_on, app.getDateString())
            }
            isSplitChip.isVisible = isSplit
            packageNameLabel.text = packageName
            versionNameLabel.text = "v${versionName}"
            apkSizeLabel.text = getString(R.string.apk_size, apkSize.bytesToMegaBytesString())
            targetApiLabel.text = getString(R.string.target_sdk, targetSdk)
            minApiLabel.text = getString(R.string.min_sdk, minSdk)
        }
    }

    private fun ChipGroup.addArchChipsToChipGroup(archNames: List<String>) {
        if (archNames.isNotEmpty()) {
            archNames.forEach { archName ->
                val chip = Chip(context, null, R.style.Widget_SimpleBackup_Chip)
                chip.text = archName
                addView(chip)
            }
        } else {
            val chip = Chip(context, null, R.style.Widget_SimpleBackup_Chip)
            chip.text = getString(R.string.all_arch)
            addView(chip)
        }
        fadeIn(300L)
    }

    private fun Menu.setFavoriteIcon() {
        findItem(R.id.add_to_favorites).apply {
            detailsViewModel.app?.apply {
                icon = if (favorite) AppCompatResources.getDrawable(
                    this@AppDetailActivity, R.drawable.ic_favorite
                )
                else AppCompatResources.getDrawable(
                    this@AppDetailActivity, R.drawable.ic_unstarred
                )
                tooltipText = if (favorite) getString(R.string.remove_from_favorites)
                else getString(R.string.add_to_favorites)
            }
        }
    }

    private fun startLocalWork() {
        detailsViewModel.app?.apply {
            if (isLocal) startProgressActivity(
                arrayOf(this.packageName), AppDataType.LOCAL
            )
            else startProgressActivity(
                arrayOf(this.packageName), AppDataType.USER
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers(packageReceiver)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> if (resultCode == RESULT_OK && data != null) {
                handleSignInIntent(data)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Suppress("DEPRECATION")
    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
            requestEmail()
            requestScopes(Scope(DriveScopes.DRIVE_FILE))
        }.build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun handleSignInIntent(data: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(data).addOnSuccessListener { googleAccount ->
            Log.d(TAG, "Signed in as " + googleAccount.email)
            val credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account
            val googleDriveService = Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Simple Backup/1.0").build()
        }.addOnFailureListener { exception: Exception? ->
            Log.e(TAG, "Unable to sign in.", exception)
        }
    }
}