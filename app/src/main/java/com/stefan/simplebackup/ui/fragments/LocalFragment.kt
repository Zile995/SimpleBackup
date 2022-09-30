package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.os.FileObserver
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.stefan.simplebackup.databinding.FragmentLocalBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.LocalAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocalFragment : BaseFragment<FragmentLocalBinding>() {
    private val localViewModel: LocalViewModel by viewModels(
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
        LocalAdapter(
            mainViewModel.selectionList,
            mainViewModel.setSelectionMode
        ) { onClickListener }

    private fun FragmentLocalBinding.bindViews() {
        bindSwipeContainer()
    }

    private fun FragmentLocalBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            launchOnViewLifecycle {
                localViewModel.refreshBackupList().invokeOnCompletion {
                    swipeRefresh.isRefreshing = false
                }
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
                            adapter.submitList(appList.sortedBy { it.name })
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