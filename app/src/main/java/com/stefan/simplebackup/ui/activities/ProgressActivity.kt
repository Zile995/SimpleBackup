package com.stefan.simplebackup.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkInfo
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.data.workers.REQUEST_TAG
import com.stefan.simplebackup.data.workers.WORK_PROGRESS
import com.stefan.simplebackup.databinding.ActivityProgressBinding
import com.stefan.simplebackup.ui.viewmodels.ProgressViewModel
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.loadBitmap
import com.stefan.simplebackup.utils.extensions.viewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {

    // Binding properties
    private val binding: ActivityProgressBinding by viewBinding(ActivityProgressBinding::inflate)

    private var isInProgress: Boolean = true
    private var bitmap: ByteArray = byteArrayOf()

    private val progressViewModel: ProgressViewModel by viewModels {
        val selection = intent?.extras?.getIntArray(SELECTION_EXTRA)
        ViewModelFactory(application as MainApplication, selection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            savedInstanceState.restoreSavedData()
            bindViews()
            initObservers()
        }
    }

    override fun onBackPressed() {
        println("OnBackPressed check: Is work in progress?: $isInProgress")
        if (!isInProgress)
            super.onBackPressed()
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
        lifecycleScope.launch {
            launch {
                progressViewModel.getWorkManager.getWorkInfosByTagLiveData(REQUEST_TAG)
                    .observe(this@ProgressActivity, workInfoObserver())
            }
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                MainWorker.notificationObserver.collect{ notificationData ->
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
        window.setBackgroundDrawableResource(R.color.background)
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
                onBackPressed()
            }
        }
    }
}