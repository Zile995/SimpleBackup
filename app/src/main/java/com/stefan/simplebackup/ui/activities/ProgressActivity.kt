package com.stefan.simplebackup.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.workers.BACKUP_PROGRESS
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.databinding.ActivityProgressBinding
import com.stefan.simplebackup.utils.backup.REQUEST_TAG
import com.stefan.simplebackup.utils.main.PreferenceHelper
import com.stefan.simplebackup.utils.main.PreferenceHelper.getPackageName
import com.stefan.simplebackup.utils.main.loadBitmapToImageView
import com.stefan.simplebackup.viewmodels.ProgressViewModel
import com.stefan.simplebackup.viewmodels.ProgressViewModelFactory
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {

    // Binding properties
    private var _binding: ActivityProgressBinding? = null
    private val binding get() = _binding!!

    private var isInProgress: Boolean = true

    private val preferencesListener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { preference, _ ->
            lifecycleScope.launch {
                preference.getPackageName()?.let { packageName ->
                    binding.setViewData(packageName)
                }
            }
        }
    }

    private val progressViewModel: ProgressViewModel by viewModels {
        val selection = intent?.extras?.getStringArray("selection_list")
        ProgressViewModelFactory(selection, application as MainApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            isInProgress = savedInstanceState.getBoolean("isInProgress")
        }

        println("Saved progress onCreate: $isInProgress")

        binding.apply {
            bindViews()
            setViewModelObservers()
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceHelper.registerPreferenceListener(preferencesListener)
    }

    override fun onBackPressed() {
        println("Saved progress back button: $isInProgress")
        if (!isInProgress)
            super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isInProgress", isInProgress)
        super.onSaveInstanceState(outState)
    }

    private fun ActivityProgressBinding.setViewModelObservers() {
        progressViewModel.getWorkManager.getWorkInfosByTagLiveData(REQUEST_TAG)
            .observe(this@ProgressActivity, workInfoObserver())
    }

    private fun ActivityProgressBinding.workInfoObserver(): Observer<List<WorkInfo>> {
        return Observer { workInfoList ->
            if (workInfoList.isEmpty())
                return@Observer
            workInfoList[0]
                .progress
                .getInt(BACKUP_PROGRESS, 0).apply {
                    progressIndicator.setProgress(this, true)
                }
            if (workInfoList[0].state.isFinished) {
                backButton.isEnabled = true
                isInProgress = false
                progressIndicator.setProgress(PROGRESS_MAX, true)
                progressViewModel.getWorkManager.pruneWork()
            }
        }
    }

    private fun ActivityProgressBinding.bindViews() {
        lifecycleScope.launch {
            window.setBackgroundDrawableResource(R.color.background)
            bindData()
            bindBackButton()
        }
    }

    private suspend fun ActivityProgressBinding.bindData() {
        PreferenceHelper.packageName?.let { packageName ->
            setViewData(packageName)
        }
    }

    private fun ActivityProgressBinding.bindBackButton() {
        backButton.isEnabled = !isInProgress
        backButton.apply {
            setOnClickListener {
                onBackPressed()
            }
        }
    }

    private suspend fun ActivityProgressBinding.setViewData(packageName: String) {
        val app = progressViewModel.getCurrentApp(packageName)
        applicationNameProgress.text = app.name
        loadBitmapToImageView(app.bitmap, applicationImageProgress)
    }

    override fun onDestroy() {
        _binding = null
        PreferenceHelper.unregisterPreferenceListener(preferencesListener)
        super.onDestroy()
    }
}