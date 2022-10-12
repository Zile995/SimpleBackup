package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.stefan.simplebackup.databinding.FragmentHomeBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.HomeAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import kotlinx.coroutines.delay

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
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

    override fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter =
        HomeAdapter(mainViewModel.selectionList, mainViewModel.setSelectionMode, onClickListener)

    private fun FragmentHomeBinding.bindViews() {
        bindSwipeContainer()
    }

    private fun FragmentHomeBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            launchOnViewLifecycle {
                homeViewModel.refreshPackages()
                delay(250)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun FragmentHomeBinding.initObservers() {
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

    override fun FragmentHomeBinding.saveRecyclerViewState() {
        homeRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentHomeBinding.restoreRecyclerViewState() {
        homeRecyclerView.restoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
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