package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.FragmentLocalBinding
import com.stefan.simplebackup.ui.adapters.LocalAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocalFragment : BaseFragment<FragmentLocalBinding>() {
    private var _localAdapter: LocalAdapter? = null
    private val localAdapter get() = _localAdapter!!

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

    override fun onResume() {
        super.onResume()
        binding.setActivityCallBacks()
    }

    private fun RecyclerView.setLocalAdapter() {
        _localAdapter = LocalAdapter(
            localViewModel.selectionList,
            localViewModel.setSelectionMode
        ) {
            val context = requireContext()
            object : OnClickListener {
                override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = localAdapter.currentList[position]
                    if (localAdapter.hasSelectedItems()) {
                        localAdapter.doSelection(holder as BaseViewHolder, item)
                    } else {
                        context.workerDialog(
                            title = getString(R.string.confirm_restore),
                            message = getString(R.string.restore_confirmation_message),
                            positiveButtonText = getString(R.string.yes),
                            negativeButtonText = getString(R.string.no)
                        ) {
                            localViewModel.startRestoreWorker(item.uid)
                        }
                    }
                }

                override fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = localAdapter.currentList[position]
                    localViewModel.setSelectionMode(true)
                    localAdapter.doSelection(holder as BaseViewHolder, item)
                }
            }
        }
        adapter = localAdapter
    }

    private fun FragmentLocalBinding.bindViews() {
        bindSwipeContainer()
        bindRecyclerView()
    }

    private fun FragmentLocalBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            onViewLifecycleScope {
                val refresh = launch {
                    // TODO: Could be deleted
                }
                refresh.join()
                swipeRefresh.isRefreshing = false
                delay(250)
            }
        }
    }

    private fun FragmentLocalBinding.bindRecyclerView() {
        localRecyclerView.apply {
            setLocalAdapter()
            setHasFixedSize(true)
        }
    }

    private fun FragmentLocalBinding.initObservers() {
        onViewLifecycleScope {
            launch {
                repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                    localViewModel.startPackagePolling()
                }
            }
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                launch {
                    localViewModel.isSelected.collect { isSelected ->
                        batchRestore.isVisible = isSelected
                    }
                }
                localViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning) {
                        localViewModel.observableList.collect { appList ->
                            localAdapter.submitList(appList)
                        }
                    }
                }
            }
        }
    }

    private fun FragmentLocalBinding.setActivityCallBacks() {
        onMainActivityCallback {
            localRecyclerView.controlFloatingButton()
        }
    }

    override fun FragmentLocalBinding.saveRecyclerViewState() {
        localRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            localViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentLocalBinding.restoreRecyclerViewState() {
        localRecyclerView.onRestoreRecyclerViewState(localViewModel.savedRecyclerViewState)
    }

    override fun onCleanUp() {
        super.onCleanUp()
        _localAdapter = null
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