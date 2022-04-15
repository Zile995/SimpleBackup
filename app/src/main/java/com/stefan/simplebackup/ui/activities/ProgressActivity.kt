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
import com.stefan.simplebackup.databinding.ActivityProgressBinding
import com.stefan.simplebackup.utils.backup.BACKUP_REQUEST_TAG
import com.stefan.simplebackup.utils.main.loadBitmapToImageView
import com.stefan.simplebackup.viewmodels.ProgressViewModel
import com.stefan.simplebackup.viewmodels.ProgressViewModelFactory
import com.stefan.simplebackup.workers.PROGRESS_MAX
import com.stefan.simplebackup.workers.Progress
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {

    // Binding properties
    private var _binding: ActivityProgressBinding? = null
    private val binding get() = _binding!!

    private var isInProgress: Boolean = false

    private val preferences: SharedPreferences by lazy {
        getSharedPreferences("package", MODE_PRIVATE)
    }

    private val preferencesListener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { preference, _ ->
            lifecycleScope.launch {
                preference.getString("package_name", null)?.let { packageName ->
                    setTheAppInfo(packageName)
                }
            }
        }
    }

    private val progressViewModel: ProgressViewModel by viewModels {
        val selection = intent?.extras?.getStringArray("selection_list")
        val application = application as MainApplication
        ProgressViewModelFactory(selection, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            isInProgress = savedInstanceState.getBoolean("isInProgress")
        }

        binding.apply {
            bindViews()
            setViewModelObservers()
        }
        createLocalBackup()
    }

    override fun onResume() {
        super.onResume()
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    override fun onBackPressed() {
        if (isInProgress) {
            return
        } else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isInProgress", isInProgress)
        super.onSaveInstanceState(outState)
    }

    private fun ActivityProgressBinding.setViewModelObservers() {
        progressViewModel.getWorkManager.getWorkInfosByTagLiveData(BACKUP_REQUEST_TAG)
            .observe(this@ProgressActivity, workInfoObserver())

    }

    private fun ActivityProgressBinding.workInfoObserver(): Observer<List<WorkInfo>> {
        return Observer { workInfoList ->
            if (workInfoList.isEmpty())
                return@Observer
            workInfoList[0]
                .progress
                .getInt(Progress, 0).apply {
                    progressIndicator.setProgress(this, true)
                }
            if (workInfoList[0].state.isFinished) {
                progressViewModel.getWorkManager.pruneWork()
                isInProgress = false
                backButton.isEnabled = true
                progressIndicator.setProgress(PROGRESS_MAX, true)
            }
        }
    }

    private fun createLocalBackup() {
        if (isInProgress) {
            return
        } else {
            isInProgress = true
            progressViewModel.createLocalBackup()
        }
    }

    private fun ActivityProgressBinding.bindViews() {
        lifecycleScope.launch {
            window.setBackgroundDrawableResource(R.color.background)
            bindBackButton()
            preferences.getString("package_name", null)?.let { packageName ->
                    setTheAppInfo(packageName)
                }
        }
    }

    private fun ActivityProgressBinding.bindBackButton() {
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private suspend fun setTheAppInfo(packageName: String) {
        val app = progressViewModel.getCurrentApp(packageName)
        binding.applicationNameProgress.text = app.name
        loadBitmapToImageView(app.bitmap, binding.applicationImageProgress)
    }

    override fun onDestroy() {
        _binding = null
        preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
        super.onDestroy()
    }
}