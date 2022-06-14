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
                setActivityCallBacks()
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

    private fun RecyclerView.setBaseAdapter() {
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
        adapter = homeAdapter
    }

    private fun FragmentHomeBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
        bindBackupChip()
    }

    private fun FragmentHomeBinding.bindRecyclerView() {
        homeRecyclerView.apply {
            setBaseAdapter()
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

    private fun FragmentHomeBinding.saveRecyclerViewState() {
        homeRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    private fun FragmentHomeBinding.restoreRecyclerViewState() {
        homeRecyclerView.onRestoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
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
        Log.d("HomeFragment", "Destroying HomeFragment")
        super.onDestroyView()
        _homeAdapter = null
        _binding = null
    }
}