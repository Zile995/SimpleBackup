package com.stefan.simplebackup.ui.activities

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.PARCELABLE_EXTRA
import com.stefan.simplebackup.databinding.ActivityDetailBinding
import com.stefan.simplebackup.ui.viewmodels.DetailsViewModel
import com.stefan.simplebackup.ui.viewmodels.DetailsViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.work.FileUtil
import com.stefan.simplebackup.utils.work.JSON_FILE_EXTENSION
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class DetailActivity : BaseActivity() {
    private val binding by viewBinding(ActivityDetailBinding::inflate)

    // AlertDialog
    private var _deleteAlertDialog: AlertDialog? = null

    // Boolean flags
    private var isReversing = false
    private var isCloudButtonClicked = false

    // DetailActivity ViewModel
    private val detailsViewModel: DetailsViewModel by viewModels {
        val selectedApp = intent.extras?.parcelable<AppData>(PARCELABLE_EXTRA)
        DetailsViewModelFactory(app = selectedApp)
    }

    private val signInIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                handleSignInIntent(
                    signInData = result.data!!,
                    onSuccess = {
                        startWork(shouldBackupToCloud = true)
                    },
                    onFailure = {
                        showToast(R.string.unable_to_sign_in)
                        Log.e("GoogleSignIn", "Sing in error: $it")
                    })
            }
        }

    // Permission launchers
    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                requestSignIn()
            } else {
                showStoragePermissionDialog()
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (!isCloudButtonClicked)
                    startWork(shouldBackupToCloud = false)
            } else {
                showStoragePermissionDialog()
            }
        }

    // Receivers
    private val packageReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                launchOnViewLifecycle {
                    if (isPackageChanged(intent)) {
                        onBackPress()
                    }
                }
            }
        }
    }

    private val barAnimator by lazy {
        ObjectAnimator.ofObject(
            window,
            "statusBarColor",
            ArgbEvaluator(),
            window.statusBarColor,
            getColorFromResource(R.color.bottom_view)
        ).apply {
            addUpdateListener {
                binding.detailsToolbar.setBackgroundColor(it.animatedValue as Int)
            }
            doOnCancel { isReversing = false }
            doOnEnd { isReversing = false }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setStatusBarColor(android.R.color.transparent)
        binding.apply {
            bindViews()
            initObservers()
        }
        registerPackageReceiver()
    }

    private fun ActivityDetailBinding.bindViews() {
        launchOnViewLifecycle {
            bindToolbar()
            bindAppBarLayout()
            bindDeleteButton()
            bindCloudBackupButton()
            bindLocalBackupButton()
            setViewsData()
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
                    val scaleFactor = 1f - offsetFactor * 0.75f
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
                }
                totalScrollRange -> {
                    startAnimation()
                    applicationImage.alpha = 0f
                    applicationImage.scaleX = 0f
                    applicationImage.scaleY = 0f
                    mainActions.translationY = totalScrollRange.toFloat()
                }
            }
            if (absoluteOffsetValue < totalScrollRange && absoluteOffsetValue < previousOffset) {
                reverseAnimation()
            }
            previousOffset = absoluteOffsetValue
        }
    }

    private fun startAnimation() {
        if (window.statusBarColor != getColorFromResource(R.color.bottom_view)) {
            barAnimator.apply {
                duration = 300L
                cancel()
                start()
            }
        }
    }

    private fun reverseAnimation() {
        if (!isReversing && window.statusBarColor != getColorFromResource(android.R.color.transparent)) {
            barAnimator.apply {
                isReversing = true
                duration = 150L
                reverse()
            }
        }
    }

    private fun ActivityDetailBinding.bindToolbar() {
        detailsToolbar.apply {
            inflateMenu(R.menu.details_tool_bar)
            setupNavAndMenu()
            menu.setMoreOptions()
            menu.setFavoriteIcon()
        }
    }

    private fun Toolbar.setupNavAndMenu() {
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
                        if (isLocal) return@run false
                        detailsViewModel.changeFavorites(
                            onSuccess = { isFavorite ->
                                menu?.setFavoriteIcon()
                                if (isFavorite)
                                    showToast(R.string.added_to_favorites)
                                else
                                    showToast(R.string.removed_from_favorites)
                            }, onFailure = { message ->
                                showToast(
                                    getString(
                                        R.string.unable_to_add_to_favorites, message
                                    )
                                )
                            })
                    }
                }
                true
            } ?: false
        }
    }

    private fun Menu.setMoreOptions() {
        detailsViewModel.app?.apply {
            findItem(R.id.force_stop).isVisible = !isLocal
            findItem(R.id.settings_info).isVisible = !isLocal
        }
    }

    private fun Menu.setFavoriteIcon() {
        findItem(R.id.add_to_favorites).apply {
            detailsViewModel.app?.apply {
                icon = if (isFavorite)
                    AppCompatResources.getDrawable(applicationContext, R.drawable.ic_favorite)
                else
                    AppCompatResources.getDrawable(applicationContext, R.drawable.ic_unstarred)
                tooltipText = when {
                    isLocal -> {
                        if (isFavorite)
                            getString(R.string.favorite_backup)
                        else {
                            isVisible = false
                            null
                        }
                    }
                    isFavorite -> getString(R.string.remove_from_favorites)
                    else -> getString(R.string.add_to_favorites)
                }
            }
        }
    }

    private fun ActivityDetailBinding.bindLocalBackupButton() {
        detailsViewModel.app?.apply {
            if (isLocal) {
                localWorkButton.setImageResource(R.drawable.ic_restore)
                localWorkButton.tooltipText = getString(R.string.restore)
            }
            localWorkButton.setOnClickListener {
                isCloudButtonClicked = false
                requestStoragePermission(storagePermissionLauncher,
                    onPermissionAlreadyGranted = {
                        startWork(shouldBackupToCloud = false)
                    })
            }
        }
    }

    private fun ActivityDetailBinding.bindCloudBackupButton() {
        detailsViewModel.app?.apply {
            cloudBackupButton.isVisible = !isLocal
            cloudBackupButton.setOnClickListener {
                isCloudButtonClicked = true
                proceedWithPermission(MainPermission.MANAGE_ALL_FILES,
                    onPermissionGranted = {
                        requestContactsPermission(
                            contactsPermissionLauncher,
                            onPermissionAlreadyGranted = {
                                requestSignIn()
                            })
                    },
                    onPermissionDenied = {
                        requestStoragePermission(storagePermissionLauncher)
                    })
            }
        }
    }

    private fun requestSignIn() {
        requestSignIn(
            resultLauncher = signInIntentLauncher,
            onAlreadySignedIn = {
                startWork(shouldBackupToCloud = true)
            })
    }

    private fun ActivityDetailBinding.bindDeleteButton() {
        detailsViewModel.app?.apply {
            if (isLocal) deleteButton.tooltipText = getString(R.string.delete_backup)
            deleteButton.setOnClickListener {
                if (isLocal)
                    _deleteAlertDialog = materialDialog(
                        title = name,
                        message = getString(R.string.delete_backup_question),
                        positiveButtonText = getString(R.string.ok),
                        negativeButtonText = getString(R.string.cancel),
                        onPositiveButtonPress = {
                            detailsViewModel.deleteLocalBackup(onSuccess = {
                                showToast(getString(R.string.backup_deleted_successfully, name))
                            }, onFailure = { message ->
                                showToast(
                                    getString(
                                        R.string.backup_deleted_successfully,
                                        "$name $message"
                                    )
                                )
                            }).invokeOnCompletion {
                                onBackPress()
                            }
                        }
                    ).apply { show() }
                else
                    uninstallPackage(packageName)
            }
        }
    }

    private suspend fun ActivityDetailBinding.setCollapsingToolbarData() {
        detailsViewModel.app?.apply {
            // Set Toolbar title
            collapsingToolbar.title = name

            // Set image onClick listener
            applicationImage.setOnClickListener {
                if (isLocal)
                    openFilePath("${FileUtil.localDirPath}/$packageName")
                else
                    launchPackage(packageName) ?: run {
                        showToast(R.string.unable_to_launch_app)
                    }
            }

            // Set bitmap image
            setBitmapFromContextDir(context = applicationContext, onFailure = {
                getResourceDrawable(R.drawable.ic_error)?.toByteArray() ?: byteArrayOf()
            })
            applicationImage.loadBitmap(bitmap)
        }
    }

    private suspend fun ActivityDetailBinding.setViewsData() {
        detailsViewModel.app?.apply {
            // Set collapsing toolbar data (title, image, and image listener)
            setCollapsingToolbarData()

            // Set type chip data
            detailsLayout.apply {
                appTypeChip.text = when {
                    isLocal -> resources.getString(R.string.local_backup)
                    else -> resources.getString(R.string.user_app)
                }

                // Set installed date
                installedDateLabel.text = when {
                    isLocal -> getString(R.string.backed_up_on, getDateText())
                    else -> getString(R.string.first_installed_on, getDateText())
                }

                // Set text for specific text views.
                isSplitChip.isVisible = isSplit
                packageNameLabel.text = packageName
                versionNameLabel.text = getString(R.string.version, versionName)
                apkSizeLabel.text = getString(R.string.apk_size, apkSize.bytesToMegaBytesString())
                targetApiLabel.text = getString(R.string.target_sdk, targetSdk)
                minApiLabel.text = getString(R.string.min_sdk, minSdk)
            }
            delay(500L)
            imageShimmer.hideShimmer()
            detailsShimmer.hideShimmer()
            detailsShimmer.isVisible = false
            detailsLayout.root.isVisible = true
        }
    }

    private fun ChipGroup.addChipsToGroup(nativeLibs: List<String>) {
        if (nativeLibs.isNotEmpty()) {
            nativeLibs.forEach { archName ->
                val chip = Chip(context, null, R.style.Widget_SimpleBackup_Chip)
                chip.text = archName
                addView(chip)
            }
        } else {
            val chip = Chip(context, null, R.style.Widget_SimpleBackup_Chip)
            chip.text = getString(R.string.no_native_libs)
            addView(chip)
        }
        fadeIn()
    }

    private fun startWork(shouldBackupToCloud: Boolean = false) {
        detailsViewModel.app?.run {
            val packageNames = arrayOf(packageName)
            val appDataType = when {
                isLocal -> AppDataType.LOCAL
                shouldBackupToCloud -> AppDataType.CLOUD
                else -> AppDataType.USER
            }
            launchProgressActivity(packageNames, appDataType)
        }
    }

    private fun ActivityDetailBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnCreated {
                detailsViewModel.apply {
                    launch {
                        localBackupFileEvents?.collect { fileEvent ->
                            Log.d("ViewModel", "DetailsViewModel fileEvent = $fileEvent")
                            fileEvent.apply {
                                if (file.extension == JSON_FILE_EXTENSION || file.name == packageName) {
                                    finish()
                                }
                            }
                        }
                    }
                    launch {
                        apkSizeStats.collect { sizeStats ->
                            sizeStats?.apply {
                                detailsLayout.dataSizeLabel.text = getString(
                                    R.string.data_size,
                                    (sizeStats.dataSize + sizeStats.cacheSize).bytesToMegaBytesString()
                                )
                            }
                        }
                    }
                    nativeLibs.collect { nativeLibs ->
                        nativeLibs?.let {
                            detailsLayout.architectureChipGroup.addChipsToGroup(nativeLibs)
                        }
                    }
                }
            }
        }
    }

    private fun registerPackageReceiver() {
        if (detailsViewModel.app?.isLocal == false) {
            registerReceiver(packageReceiver, intentFilter(
                Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED
            ) {
                addDataScheme("package")
            })
        }
    }

    private fun isPackageChanged(intent: Intent): Boolean {
        val isCorrectPackage =
            intent.data?.encodedSchemeSpecificPart == detailsViewModel.app?.packageName
        val isPackageChanged =
            intent.isPackageAdded() || intent.isPackageRemoved()
        return isCorrectPackage && isPackageChanged
    }

    override fun onDestroy() {
        super.onDestroy()
        _deleteAlertDialog?.dismiss()
        _deleteAlertDialog = null
        if (detailsViewModel.app?.isLocal == false) {
            unregisterReceivers(packageReceiver)
        }
    }
}