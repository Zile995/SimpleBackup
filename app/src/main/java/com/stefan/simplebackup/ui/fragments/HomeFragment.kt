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
import com.stefan.simplebackup.databinding.FragmentHomeBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.activities.ProgressActivity
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.HolderType
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.utils.extensions.*
import com.stefan.simplebackup.viewmodels.HomeViewModel
import com.stefan.simplebackup.viewmodels.HomeViewModelFactory
import com.stefan.simplebackup.viewmodels.SELECTION_EXTRA
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    // Binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var _homeAdapter: BaseAdapter? = null
    private val homeAdapter get() = _homeAdapter!!

    // ViewModel
    private val homeViewModel: HomeViewModel by activityViewModels {
        HomeViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "Creating HomeFragment")
        _binding = FragmentHomeBinding
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
                setActivityCallBacks()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.apply {
            saveRecyclerViewState()
        }
    }

    private fun setHomeAdapter() {
        _homeAdapter =
            BaseAdapter(
                HolderType.HOME,
                homeViewModel.selectionList,
                homeViewModel.setSelectionMode
            ).apply {
                clickListener = object : OnClickListener {
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
    }

    private fun FragmentHomeBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
        bindBackupChip()
    }

    private fun FragmentHomeBinding.bindRecyclerView() {
        setHomeAdapter()
        recyclerView.apply {
            adapter = homeAdapter
            setHasFixedSize(true)
        }
    }

    private fun FragmentHomeBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                homeViewModel.refreshPackageList()
                println("List size: ${homeViewModel.selectionList}")
                delay(250)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun FragmentHomeBinding.setActivityCallBacks() {
        activity?.onActivityCallbacks<MainActivity> {
            recyclerView.controlFloatingButton(homeViewModel.isButtonVisible)
        }
    }

    private fun FragmentHomeBinding.bindBackupChip() {
        batchBackup.setOnClickListener {
            requireContext().apply {
                passBundleToActivity<ProgressActivity>(SELECTION_EXTRA to homeViewModel.selectionList.toIntArray())
            }
        }
    }

    private fun FragmentHomeBinding.saveRecyclerViewState() {
        recyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    private fun FragmentHomeBinding.restoreRecyclerViewState() {
        recyclerView.onRestoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _homeAdapter = null
        Log.d("HomeFragment", "Destroying HomeFragment")
    }
}