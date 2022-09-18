package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.stefan.simplebackup.databinding.FragmentCloudBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.CloudAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CloudFragment : BaseFragment<FragmentCloudBinding>() {
    private val homeViewModel: HomeViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val contactsPermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            binding.cloudRecyclerView.isVisible =  isGranted
            binding.contactsPermissionLabel.isVisible = !isGranted
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            bindViews()
            initObservers()
            restoreRecyclerViewState()
        }
    }

    private fun FragmentCloudBinding.bindViews() {
        bindSwipeContainer()
    }

    private fun FragmentCloudBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                homeViewModel.refreshPackages()
                delay(250)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter =
        CloudAdapter(
            mainViewModel.selectionList,
            mainViewModel.setSelectionMode
        ) { onClickListener }

    private fun FragmentCloudBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        homeViewModel.observableList.collect { appList ->
                            adapter.submitList(appList)
                        }
                }
            }
        }
    }

    override fun FragmentCloudBinding.saveRecyclerViewState() {
        cloudRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentCloudBinding.restoreRecyclerViewState() {
        cloudRecyclerView.restoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed CloudFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed CloudFragment completely")
    }
}