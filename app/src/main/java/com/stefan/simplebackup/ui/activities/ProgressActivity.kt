package com.stefan.simplebackup.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.APP_DATA_TYPE_EXTRA
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.data.workers.WORK_PROGRESS
import com.stefan.simplebackup.data.workers.WORK_REQUEST_TAG
import com.stefan.simplebackup.databinding.ActivityProgressBinding
import com.stefan.simplebackup.ui.viewmodels.ProgressViewModel
import com.stefan.simplebackup.ui.viewmodels.ProgressViewModelFactory
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {

    // Binding properties
    private val binding: ActivityProgressBinding by viewBinding(ActivityProgressBinding::inflate)

    private val workManager by lazy { WorkManager.getInstance(application) }

    private var isInProgress: Boolean = true
    private var bitmap: ByteArray = byteArrayOf()

    private val progressViewModel: ProgressViewModel by viewModels {
        val selectionList = intent?.extras?.getStringArray(SELECTION_EXTRA)
        val appDataType = intent?.extras?.parcelable<AppDataType>(APP_DATA_TYPE_EXTRA)
        ProgressViewModelFactory(
            selectionList = selectionList,
            appDataType = appDataType,
            application = application as MainApplication
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColorFromResource(R.color.main_background)
        setContentView(binding.root)

        binding.apply {
            savedInstanceState.restoreSavedData()
            bindViews()
            initObservers()
        }
    }

    override fun onBackPress() {
        println("OnBackPressed check: Is work in progress?: $isInProgress")
        if (!isInProgress)
            super.onBackPress()
    }

    private fun Bundle?.restoreSavedData() {
        this?.apply {
            isInProgress = getBoolean("isInProgress")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isInProgress", isInProgress)
        super.onSaveInstanceState(outState)
    }

    private fun ActivityProgressBinding.initObservers() {
        progressViewModel
        launchOnViewLifecycle {
            launch {
                workManager.getWorkInfosByTagLiveData(WORK_REQUEST_TAG)
                    .observe(this@ProgressActivity, workInfoObserver())
            }
            repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                MainWorker.notificationObserver.collect { notificationData ->
                    updateViews(notificationData)
                }
            }
        }
    }

    private fun ActivityProgressBinding.workInfoObserver(): Observer<List<WorkInfo>> {
        return Observer { workInfoList ->
            if (workInfoList.isEmpty())
                return@Observer
            workInfoList[0]
                .progress
                .getInt(WORK_PROGRESS, 0).apply {
                    progressIndicator.setProgress(this, true)
                }
            if (workInfoList[0].state.isFinished) {
                backButton.isEnabled = true
                isInProgress = false
                progressIndicator.setProgress(PROGRESS_MAX, true)
                workManager.pruneWork()
            }
        }
    }

    private fun ActivityProgressBinding.bindViews() {
        bindBackButton()
        bindProgressIndicator()
        bindProgressTypeTitle()
    }

    @SuppressLint("SetTextI18n")
    private fun ActivityProgressBinding.bindProgressTypeTitle() {
        updateProgressTypeTitle(currentItem = 1)
    }

    private fun ActivityProgressBinding.updateProgressTypeTitle(currentItem: Int) {
        val progressTitle = when (progressViewModel.appDataType) {
            AppDataType.USER -> getString(R.string.backing_up)
            AppDataType.LOCAL -> getString(R.string.restoring)
            AppDataType.CLOUD -> getString(R.string.uploading_to_cloud)
            else -> ""
        }
        progressType.text = getString(
            R.string.progress_type,
            progressTitle,
            currentItem,
            progressViewModel.numberOfItems
        )
    }

    private fun ActivityProgressBinding.updateViews(notificationData: NotificationData?) {
        notificationData?.apply {
            if (!bitmap.contentEquals(image)) {
                applicationImageProgress.loadBitmap(image)
                bitmap = image
            }
            applicationNameProgress.text = name
            applicationProgressInfo.text = this.text
            updateProgressTypeTitle(index)
        }
    }

        private fun ActivityProgressBinding.bindProgressIndicator() {
            progressIndicator.max = PROGRESS_MAX
        }

        private fun ActivityProgressBinding.bindBackButton() {
            backButton.isEnabled = !isInProgress
            backButton.apply {
                setOnClickListener {
                    onBackPress()
                }
            }
        }
    }