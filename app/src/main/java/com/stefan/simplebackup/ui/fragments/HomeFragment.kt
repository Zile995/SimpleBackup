package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.databinding.FragmentHomeBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.activities.ProgressActivity
import com.stefan.simplebackup.ui.adapters.HomeAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private var _homeAdapter: HomeAdapter? = null
    private val homeAdapter get() = _homeAdapter!!

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

    private fun RecyclerView.setHomeAdapter() {
        _homeAdapter =
            HomeAdapter(
                homeViewModel.selectionList,
                homeViewModel.setSelectionMode
            ) {
                object : OnClickListener {
                    override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                        val item = homeAdapter.currentList[position]
                        if (homeAdapter.hasSelectedItems()) {
                            homeAdapter.doSelection(holder as BaseViewHolder, item)
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
                        val item = homeAdapter.currentList[position]
                        homeViewModel.setSelectionMode(true)
                        homeAdapter.doSelection(holder as BaseViewHolder, item)
                    }
                }
            }
        adapter = homeAdapter
    }

    private fun FragmentHomeBinding.bindViews() {
        bindSwipeContainer()
        bindRecyclerView()
        bindBackupChip()
    }

    private fun FragmentHomeBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            onViewLifecycleScope {
                homeViewModel.refreshPackages()
                delay(250)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun FragmentHomeBinding.bindRecyclerView() {
        homeRecyclerView.apply {
            setHomeAdapter()
            setHasFixedSize(true)
        }
    }

    private fun FragmentHomeBinding.bindBackupChip() {
        batchBackup.setOnClickListener {
            requireContext().apply {
                passBundleToActivity<ProgressActivity>(SELECTION_EXTRA to homeViewModel.selectionList.toIntArray())
            }
        }
    }

    private fun FragmentHomeBinding.initObservers() {
        repeatOnViewLifecycleScope(Lifecycle.State.STARTED) {
            launch {
                homeViewModel.isSelected.collect { isSelected ->
                    batchBackup.isVisible = isSelected
                    onMainActivityCallback {
                        controlBottomView(!isSelected)
                    }
                }
            }
            homeViewModel.spinner.collect { isSpinning ->
                progressBar.isVisible = isSpinning
                if (!isSpinning)
                    homeViewModel.observableList.collect { appList ->
                        homeAdapter.submitList(appList)
                    }
            }
        }
    }

    private fun FragmentHomeBinding.setActivityCallBacks() {
        onMainActivityCallback {
            homeRecyclerView.controlFloatingButton()
        }
    }

    override fun FragmentHomeBinding.saveRecyclerViewState() {
        homeRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentHomeBinding.restoreRecyclerViewState() {
        homeRecyclerView.onRestoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
    }

    override fun onCleanUp() {
        super.onCleanUp()
        _homeAdapter = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed HomeFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed HomeFragment completely")
    }
}