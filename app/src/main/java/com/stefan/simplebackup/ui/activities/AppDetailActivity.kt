package com.stefan.simplebackup.ui.activities

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.PARCELABLE_EXTRA
import com.stefan.simplebackup.databinding.ActivityDetailBinding
import com.stefan.simplebackup.ui.viewmodels.DetailsViewModel
import com.stefan.simplebackup.ui.viewmodels.DetailsViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.utils.file.BitmapUtil.toByteArray
import com.stefan.simplebackup.utils.file.FileUtil
import com.stefan.simplebackup.utils.file.JSON_FILE_EXTENSION
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

class AppDetailActivity : BaseActivity() {
    private val binding by viewBinding(ActivityDetailBinding::inflate)

    private var isToolbarAnimating = false
    private var cloudBackupClicked = false

    private val detailsViewModel: DetailsViewModel by viewModels {
        val selectedApp = intent.extras?.parcelable<AppData>(PARCELABLE_EXTRA)
        DetailsViewModelFactory(app = selectedApp)
    }

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
                if (!cloudBackupClicked)
                    startWork(shouldBackupToCloud = false)
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
        setStatusBarColor(R.color.main_background)
        binding.apply {
            bindViews()
            initObservers()
        }
        registerPackageReceiver()
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

    private fun ActivityDetailBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                detailsViewModel.apply {
                    launch {
                        app?.apply {
                            if (isLocal) {
                                backupFileEvents.collect { fileEvent ->
                                    Log.d("ViewModel", "DetailsViewModel fileEvent = $fileEvent")
                                    fileEvent.apply {
                                        if (file.extension == JSON_FILE_EXTENSION || file.name == packageName) {
                                            finish()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    launch {
                        apkSizeStats.collect { sizeStats ->
                            sizeStats?.apply {
                                dataSizeLabel.text = getString(
                                    R.string.data_size,
                                    (sizeStats.dataSize + sizeStats.cacheSize).bytesToMegaBytesString()
                                )
                            }
                        }
                    }
                    nativeLibs.collect { nativeLibs ->
                        Log.d("ChipGroup", "Arch names = $nativeLibs")
                        nativeLibs?.let {
                            architectureChipGroup.addArchChipsToChipGroup(nativeLibs)
                        }
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
                    animateStatusBarColor(R.color.bottom_view)
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
            menu.setMoreOptions()
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
                            if (isLocal) return@run false
                            detailsViewModel.changeFavoritesForInstalledApp(
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
    }

    private fun ActivityDetailBinding.bindLocalBackupButton() {
        detailsViewModel.app?.apply {
            if (isLocal) {
                localWorkButton.setImageResource(R.drawable.ic_restore)
                localWorkButton.tooltipText = getString(R.string.restore)
            }
            localWorkButton.setOnClickListener {
                cloudBackupClicked = false
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
                cloudBackupClicked = true
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

    private fun ActivityDetailBinding.bindDeleteButton() {
        detailsViewModel.app?.apply {
            if (isLocal) deleteButton.tooltipText = getString(R.string.delete_backup)
            deleteButton.setOnClickListener {
                if (isLocal)
                    materialDialog(
                        title = name,
                        message = getString(R.string.delete_backup_question),
                        positiveButtonText = getString(R.string.ok),
                        negativeButtonText = getString(R.string.cancel),
                        onPositiveButtonPress = {
                            detailsViewModel.deleteLocalBackup(onSuccess = {
                                showToast(getString(R.string.backup_deleted_successfully, name))
                            }, onFailure = { message ->
                                showToast(
                                    getString(R.string.backup_deleted_successfully, "$name $message")
                                )
                            }).invokeOnCompletion {
                                onBackPress()
                            }
                        }
                    )
                else
                    uninstallPackage(packageName)
            }
        }
    }

    private suspend fun ActivityDetailBinding.setCollapsingToolbarData(app: AppData) {
        app.apply {
            collapsingToolbar.title = name
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
        }
    }

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
            versionNameLabel.text = getString(R.string.version, versionName)
            apkSizeLabel.text = getString(R.string.apk_size, apkSize.bytesToMegaBytesString())
            targetApiLabel.text = getString(R.string.target_sdk, targetSdk)
            minApiLabel.text = getString(R.string.min_sdk, minSdk)
        }
    }

    private fun ChipGroup.addArchChipsToChipGroup(nativeLibs: List<String>) {
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

    private fun startWork(shouldBackupToCloud: Boolean = false) {
        detailsViewModel.app?.run {
            val packageNames = arrayOf(packageName)
            when {
                isLocal -> startProgressActivity(packageNames, AppDataType.LOCAL)
                shouldBackupToCloud -> startProgressActivity(packageNames, AppDataType.CLOUD)
                else -> startProgressActivity(packageNames, AppDataType.USER)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInIntent(
                        signInData = data,
                        onSuccess = {
                            startWork(shouldBackupToCloud = true)
                        },
                        onFailure = {
                            showToast(R.string.unable_to_sign_in)
                            Log.e("GoogleSignIn", "${getString(R.string.unable_to_sign_in)} $it")
                        })
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (detailsViewModel.app?.isLocal == false) {
            unregisterReceivers(packageReceiver)
        }
    }
}