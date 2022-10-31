package com.stefan.simplebackup.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.APP_DATA_TYPE_EXTRA
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.data.workers.WORK_PROGRESS
import com.stefan.simplebackup.data.workers.WORK_REQUEST_TAG
import com.stefan.simplebackup.databinding.ActivityProgressBinding
import com.stefan.simplebackup.ui.adapters.ProgressAdapter
import com.stefan.simplebackup.ui.viewmodels.ProgressViewModel
import com.stefan.simplebackup.ui.viewmodels.ProgressViewModelFactory
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {

    // Binding properties
    private val binding: ActivityProgressBinding by viewBinding(ActivityProgressBinding::inflate)

    private val workManager by lazy { WorkManager.getInstance(application) }

    private var _progressAdapter: ProgressAdapter? = null
    private val progressAdapter get() = _progressAdapter!!

    private var isInProgress: Boolean = true
    private var bitmap: ByteArray = byteArrayOf()

    private val progressViewModel: ProgressViewModel by viewModels {
        val selectionList = fromIntentExtras { getStringArray(SELECTION_EXTRA) }
        val appDataType = fromIntentExtras { parcelable<AppDataType>(APP_DATA_TYPE_EXTRA) }
        ProgressViewModelFactory(
            appDataType = appDataType,
            selectionList = selectionList,
            workManager = workManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setStatusBarColor(R.color.main_background)
        savedInstanceState.restoreSavedData()
        binding.apply {
            bindViews()
            initObservers()
        }
    }

    override fun onBackPress() {
        if (!isInProgress) {
            MainWorker.clearProgressData()
            workManager.pruneWork()
            super.onBackPress()
        }
    }

    private fun Bundle?.restoreSavedData() =
        this?.let {
            isInProgress = getBoolean("isInProgress")
        }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isInProgress", isInProgress)
        super.onSaveInstanceState(outState)
    }

    private fun ActivityProgressBinding.initObservers() {
        launchOnViewLifecycle {
            launch {
                workManager.getWorkInfosByTagLiveData(WORK_REQUEST_TAG)
                    .observe(this@ProgressActivity, workInfoObserver())
            }
            repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                progressViewModel.progressDataObserver.collect { notificationData ->
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
                .getInt(WORK_PROGRESS, 0).let { currentProgress ->
                    progressIndicator.setProgress(currentProgress, true)
                }
            if (workInfoList[0].state.isFinished) {
                isInProgress = false
                backButton.isEnabled = true
                progressIndicator.setProgress(PROGRESS_MAX, true)
                progressType.text = getString(R.string.work_finished)
            }
        }
    }

    private fun ActivityProgressBinding.bindViews() {
        bindBackButton()
        bindProgressIndicator()
        bindProgressTypeTitle()
        bindProgressRecyclerView()
    }

    private fun ActivityProgressBinding.bindProgressRecyclerView() {
        progressRecyclerview.apply {
            _progressAdapter = ProgressAdapter()
            adapter = progressAdapter
            setHasFixedSize(true)
        }
    }

    private fun ActivityProgressBinding.bindProgressTypeTitle() {
        updateProgressTypeTitle(currentItem = 1)
    }

    private fun ActivityProgressBinding.updateProgressTypeTitle(currentItem: Int) {
        val workInfo = workManager.getWorkInfosByTagLiveData(WORK_REQUEST_TAG).value?.get(0)
        if (workInfo?.state?.isFinished == true) return
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

    private fun ActivityProgressBinding.updateViews(progressData: ProgressData?) {
        progressData?.apply {
            if (!bitmap.contentEquals(image)) {
                applicationImageProgress.loadBitmap(image)
                bitmap = image
            }
            applicationNameProgress.text = name
            applicationProgressInfo.text = this.message
            updateProgressTypeTitle(index)
            workResult?.let {
                progressViewModel.updateProgressDataList(progressData)
                progressAdapter.submitList(progressViewModel.progressDataList.toMutableList())
            }
        }
    }

    private fun ActivityProgressBinding.bindProgressIndicator() {
        progressIndicator.max = PROGRESS_MAX
    }

    private fun ActivityProgressBinding.bindBackButton() {
        backButton.isEnabled = !isInProgress
        backButton.setOnClickListener {
            onBackPress()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _progressAdapter = null
    }
}