package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.databinding.FragmentLocalBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.LocalAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.viewmodels.LocalViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay
import kotlin.properties.Delegates

class LocalFragment : BaseFragment<FragmentLocalBinding>() {
    private val localViewModel: LocalViewModel by viewModels {
        LocalViewModelFactory(requireActivity().application as MainApplication)
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
        onMainActivityCallback {
            requestStoragePermission(
                permissionLauncher = storagePermissionLauncher,
                onPermissionAlreadyGranted = {
                    isStoragePermissionGranted = true
                })
        }
        binding.apply {
            bindViews()
            initObservers()
            restoreRecyclerViewState()
        }
    }

    override fun onStart() {
        super.onStart()
        val appPermissionManager = AppPermissionManager(requireContext().applicationContext)
        isStoragePermissionGranted =
            appPermissionManager.checkMainPermission(MainPermission.MANAGE_ALL_FILES)
    }

    override fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter =
        LocalAdapter(mainViewModel.selectionList, mainViewModel.setSelectionMode, onClickListener)

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
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                localViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning) {
                        localViewModel.observableList.collect { appList ->
                            adapter.submitList(appList)
                            if (appList.isEmpty()) {
                                delay(250L)
                                if (isStoragePermissionGranted == true) {
                                    noBackupsLabel.fadeIn(animationDuration = 250L)
                                }
                            } else {
                                noBackupsLabel.isVisible = false
                            }
                        }
                    }
                }
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