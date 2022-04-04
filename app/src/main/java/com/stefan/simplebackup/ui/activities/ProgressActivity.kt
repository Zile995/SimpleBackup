package com.stefan.simplebackup.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.stefan.simplebackup.MainApplication
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

    private val preferences: SharedPreferences by lazy {
        getSharedPreferences("package", MODE_PRIVATE)
    }

    private val preferencesListener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { preference, _ ->
            lifecycleScope.launch {
                preference.getString("package_name", null)?.let { packageName ->
                    val app = progressViewModel.getCurrentApp(packageName)
                    binding.applicationNameProgress.text = app.name
                    loadBitmapToImageView(app.bitmap, binding.applicationImageProgress)
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

        binding.bindViews()
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        setViewModelObservers()
        progressViewModel.createLocalBackup()
    }

    override fun onResume() {
        super.onResume()
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    override fun onPause() {
        super.onPause()
        preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
    }

    private fun setViewModelObservers() {
        progressViewModel.getWorkManager.getWorkInfosByTagLiveData(BACKUP_REQUEST_TAG)
            .observe(this, workInfoObserver())
    }

    private fun workInfoObserver(): Observer<List<WorkInfo>> {
        return Observer { workInfoList ->
            if (workInfoList.isEmpty())
                return@Observer
            workInfoList[0]
                .progress
                .getInt(Progress, 0).apply {
                    binding.progressIndicator.setProgress(this, true)
                }
            if (workInfoList[0].state.isFinished) {
                progressViewModel.getWorkManager.pruneWork()
                binding.progressIndicator.setProgress(PROGRESS_MAX, true)
            }
        }
    }

    private fun ActivityProgressBinding.bindViews() {
        lifecycleScope.launchWhenCreated {

        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}