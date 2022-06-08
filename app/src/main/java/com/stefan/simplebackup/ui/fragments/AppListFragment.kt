package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.activities.ProgressActivity
import com.stefan.simplebackup.ui.adapters.AppAdapter
import com.stefan.simplebackup.ui.adapters.AppViewHolder
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.viewmodels.AppViewModel
import com.stefan.simplebackup.viewmodels.AppViewModelFactory
import com.stefan.simplebackup.viewmodels.SELECTION_EXTRA
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppListFragment : Fragment() {
    // Binding
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private var _appAdapter: AppAdapter? = null
    private val appAdapter get() = _appAdapter!!

    // ViewModel
    private val appViewModel: AppViewModel by activityViewModels {
        AppViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("AppListFragment", "Creating AppListFragment")
        _binding = FragmentAppListBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            binding.apply {
                bindViews()
                initObservers()
                restoreRecyclerViewState()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.apply {
            saveRecyclerViewState()
        }
    }

    private fun setAppAdapter() {
        val clickListener =
            object : OnClickListener {
                override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = appAdapter.currentList[position]
                    if (appAdapter.hasSelectedItems()) {
                        appAdapter.doSelection(holder as AppViewHolder, item)
                    } else {
                        viewLifecycleOwner.lifecycleScope.launch {
                            item.passToActivity<AppDetailActivity>(context)
                        }
                    }
                }

                override fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = appAdapter.currentList[position]
                    appViewModel.setSelectionMode(true)
                    appAdapter.doSelection(holder as AppViewHolder, item)
                }
            }
        _appAdapter =
            AppAdapter(appViewModel.selectionList, clickListener, appViewModel.setSelectionMode)
    }

    private fun FragmentAppListBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
        bindFloatingButton()
        bindBackupChip()
    }

    private fun FragmentAppListBinding.bindRecyclerView() {
        setAppAdapter()
        recyclerView.apply {
            adapter = appAdapter
            setHasFixedSize(true)
        }
    }

    private fun FragmentAppListBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                appViewModel.refreshPackageList()
                delay(250)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun FragmentAppListBinding.bindFloatingButton() {
        recyclerView.hideAttachedButton(floatingButton)

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    private fun FragmentAppListBinding.bindBackupChip() {
        batchBackup.setOnClickListener {
            requireContext().apply {
                passBundleToActivity<ProgressActivity>(SELECTION_EXTRA to appViewModel.selectionList.toIntArray())
            }
        }
    }

    private fun FragmentAppListBinding.saveRecyclerViewState() {
        recyclerView.onSaveRecyclerViewState { stateParcelable ->
            appViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    private fun FragmentAppListBinding.restoreRecyclerViewState() {
        recyclerView.onRestoreRecyclerViewState(appViewModel.savedRecyclerViewState)
    }

    private fun FragmentAppListBinding.initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    appViewModel.isSelected.collect { isSelected ->
                        batchBackup.isVisible = isSelected
                    }
                }
                appViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        appViewModel.installedApps.collect { appList ->
                            appAdapter.submitList(appList)
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _appAdapter = null
        Log.d("AppListFragment", "Destroying AppListFragment")
    }
}