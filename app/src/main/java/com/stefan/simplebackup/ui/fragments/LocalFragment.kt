package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentLocalBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.LocalAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.viewmodels.LocalViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.onMainActivity
import com.stefan.simplebackup.utils.extensions.repeatOnStarted
import kotlinx.coroutines.delay
import kotlin.properties.Delegates

class LocalFragment : BaseFragment<FragmentLocalBinding>() {
    private val localViewModel: LocalViewModel by viewModels {
        LocalViewModelFactory()
    }

    private val appPermissionManager by lazy {
        AppPermissionManager(requireContext().applicationContext)
    }

    private var isStoragePermissionGranted by Delegates.observable<Boolean?>(null) { _, _, isGranted ->
        controlViewsOnPermissionChange(isGranted)
        if (isGranted == false) adapter.submitList(mutableListOf())
    }

    private val storagePermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            isStoragePermissionGranted = isGranted
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions()
        binding.apply {
            bindViews()
            initObservers()
            restoreRecyclerViewState()
        }
    }

    override fun onStart() {
        super.onStart()
        isStoragePermissionGranted =
            appPermissionManager.checkMainPermission(MainPermission.MANAGE_ALL_FILES)
    }

    override fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter =
        LocalAdapter(mainViewModel.selectionList, mainViewModel.setSelectionMode, onClickListener)

    private fun requestPermissions() {
        onMainActivity {
            requestStoragePermission(
                permissionLauncher = storagePermissionLauncher,
                onPermissionAlreadyGranted = {
                    isStoragePermissionGranted = true
                })
        }
    }

    private fun FragmentLocalBinding.bindViews() {
        bindSwipeContainer()
    }

    private fun controlViewsOnPermissionChange(isGranted: Boolean?) {
        binding.apply {
            storagePermissionLabel.isVisible = isGranted == false
            localRecyclerView.isVisible = isGranted == true
        }
    }

    private fun FragmentLocalBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            launchOnViewLifecycle {
                localViewModel.refreshBackupList()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun FragmentLocalBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnStarted {
                localViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning) {
                        localViewModel.observableList.collect { appList ->
                            adapter.submitList(appList)
                            noBackupsLabel.isVisible =
                                isStoragePermissionGranted == true && appList.isEmpty()
                        }
                    }
                }
            }
        }
    }

    override fun onClickSelectionAction() {
        onMainActivity {
            launchOnViewLifecycle {
                val selectionList = mainViewModel.selectionList.toTypedArray()
                launchProgressActivity(selectionList, AppDataType.LOCAL)
                delay(250L)
                mainViewModel.setSelectionMode(false)
            }
        }
    }

    override fun FragmentLocalBinding.saveRecyclerViewState() {
        localRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            localViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentLocalBinding.restoreRecyclerViewState() {
        localRecyclerView.restoreRecyclerViewState(localViewModel.savedRecyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed LocalFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed LocalFragment completely")
    }
}