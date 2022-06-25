package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentHomeBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.activities.ProgressActivity
import com.stefan.simplebackup.ui.adapters.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.HomeAdapter
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.viewmodels.HomeViewModelFactory
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    // Binding
    private val binding by viewBinding(FragmentHomeBinding::inflate) {
        cleanUp()
    }
    private var _homeAdapter: HomeAdapter? = null
    private val homeAdapter get() = _homeAdapter!!

    // ViewModel
    private val homeViewModel: HomeViewModel by activityViewModels {
        HomeViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingReference = WeakReference(binding)
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
        adapter = _homeAdapter
    }

    private fun FragmentHomeBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
        bindBackupChip()
    }

    private fun FragmentHomeBinding.bindRecyclerView() {
        homeRecyclerView.apply {
            setHomeAdapter()
            setHasFixedSize(true)
        }
    }

    private fun FragmentHomeBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                homeViewModel.refreshPackageList()
                delay(250)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun FragmentHomeBinding.setActivityCallBacks() {
        onActivityCallback<MainActivity> {
            homeRecyclerView.controlFloatingButton()
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    homeViewModel.isSelected.collect { isSelected ->
                        batchBackup.isVisible = isSelected
                    }
                }
                homeViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        homeViewModel.installedApps.collect { appList ->
                            homeAdapter.submitList(appList)
                        }
                }
            }
        }
    }

    override fun WeakReference<FragmentHomeBinding>.saveRecyclerViewState() {
        val binding = this.get()
        binding?.apply {
            homeRecyclerView.onSaveRecyclerViewState { stateParcelable ->
                homeViewModel.saveRecyclerViewState(stateParcelable)
            }
        }
    }

    override fun FragmentHomeBinding.restoreRecyclerViewState() {
        homeRecyclerView.onRestoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
    }

    private fun cleanUp() {
        _homeAdapter = null
    }

    override fun onDestroyView() {
        Log.d("HomeFragment", "Destroyed HomeFragment Views")
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HomeFragment", "Destroyed HomeFragment completely")
    }
}