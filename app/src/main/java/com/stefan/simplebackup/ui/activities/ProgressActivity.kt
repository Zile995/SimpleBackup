package com.stefan.simplebackup.ui.activities

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
import com.stefan.simplebackup.data.workers.REQUEST_TAG
import com.stefan.simplebackup.data.workers.WORK_PROGRESS
import com.stefan.simplebackup.databinding.ActivityProgressBinding
import com.stefan.simplebackup.ui.viewmodels.ProgressViewModel
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {

    // Binding properties
    private val binding: ActivityProgressBinding by viewBinding(ActivityProgressBinding::inflate)

    private var isInProgress: Boolean = true
    private var bitmap: ByteArray = byteArrayOf()

    private val progressViewModel: ProgressViewModel by viewModels {
        val selection = intent?.extras?.getStringArray(SELECTION_EXTRA)
        val appDataType = intent?.extras?.parcelable<AppDataType>(APP_DATA_TYPE_EXTRA)
        ViewModelFactory(application as MainApplication, selection, appDataType)
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
                WorkManager.getInstance(application).getWorkInfosByTagLiveData(REQUEST_TAG)
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
            }
        }
    }

    private fun ActivityProgressBinding.bindViews() {
        bindProgressIndicator()
        bindBackButton()
    }

    private fun ActivityProgressBinding.updateViews(notificationData: NotificationData?) {
        notificationData?.apply {
            if (!bitmap.contentEquals(image)) {
                applicationImageProgress.loadBitmap(image)
                applicationNameProgress.text = name
                bitmap = image
            }
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