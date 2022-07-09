package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.databinding.FragmentCloudBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.adapters.CloudAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CloudFragment : BaseFragment<FragmentCloudBinding>() {
    private var _cloudAdapter: CloudAdapter? = null
    private val cloudAdapter get() = _cloudAdapter!!

    private val homeViewModel: HomeViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            bindViews()
            initObservers()
            restoreRecyclerViewState()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.setActivityCallBacks()
    }

    private fun FragmentCloudBinding.bindViews() {
        bindSwipeContainer()
        bindRecyclerView()
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

    private fun FragmentCloudBinding.bindRecyclerView() {
        cloudRecyclerView.apply {
            setCloudAdapter()
            setHasFixedSize(true)
        }
    }

    private fun RecyclerView.setCloudAdapter() {
        _cloudAdapter =
            CloudAdapter(
                homeViewModel.selectionList,
                homeViewModel.setSelectionMode
            ) {
                object : OnClickListener {
                    override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                        val item = cloudAdapter.currentList[position]
                        if (cloudAdapter.hasSelectedItems()) {
                            cloudAdapter.doSelection(holder as BaseViewHolder, item)
                        } else {
                            viewLifecycleOwner.lifecycleScope.launch {
                                item.passToActivity<AppDetailActivity>(context)
                            }
                        }
                    }

                    override fun onLongItemViewClick(
                        holder: RecyclerView.ViewHolder,
                        position: Int
                    ) {
                        val item = cloudAdapter.currentList[position]
                        homeViewModel.setSelectionMode(true)
                        cloudAdapter.doSelection(holder as BaseViewHolder, item)
                    }
                }
            }
        adapter = cloudAdapter
    }

    private fun FragmentCloudBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.isSelected.collect { isSelected ->
                        mainViewModel.changeTab(isSelected)
                        batchBackup.isVisible = isSelected
                    }
                }
                homeViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        homeViewModel.observableList.collect { appList ->
                            cloudAdapter.submitList(appList)
                        }
                }
            }
        }
    }

    private fun FragmentCloudBinding.setActivityCallBacks() {
        onMainActivityCallback {
            cloudRecyclerView.controlFloatingButton()
        }
    }

    override fun FragmentCloudBinding.saveRecyclerViewState() {
        cloudRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentCloudBinding.restoreRecyclerViewState() {
        cloudRecyclerView.onRestoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed CloudFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed CloudFragment completely")
    }

    override fun onCleanUp() {
        super.onCleanUp()
        _cloudAdapter = null
    }
}