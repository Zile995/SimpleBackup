package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.stefan.simplebackup.databinding.FragmentSearchBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.SearchAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.SearchViewModel
import com.stefan.simplebackup.ui.viewmodels.SearchViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.launchPostDelayed
import com.stefan.simplebackup.utils.extensions.onMainActivity
import kotlin.properties.Delegates

class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private val searchViewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(mainViewModel.repository)
    }

    private var shouldObserveLocal: Boolean? by Delegates.observable(null) { _, oldCheckedValue, isLocalChecked ->
        if (isLocalChecked == null) return@observable
        if (isLocalChecked != oldCheckedValue) binding.startObserving(isLocalChecked)
    }

    init {
        shouldEnableOnLongClick = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnBackPressedCallback()
        binding.bindChipGroup()
    }

    private fun setOnBackPressedCallback() {
        onMainActivity {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                onSupportNavigateUp()
            }
        }
    }

    private fun FragmentSearchBinding.bindChipGroup() {
        setCheckedChip()
        searchChipGroup.children.forEach { chipView ->
            val chip = chipView as Chip
            chip.setOnCheckedChangeListener { buttonView, _ ->
                val index = searchChipGroup.indexOfChild(buttonView)
                searchViewModel.saveCheckedChipPosition(index)
                if (chip.isChecked) return@setOnCheckedChangeListener
                searchChipGroup.removeView(buttonView)
                searchChipGroup.addView(buttonView, index)
            }
        }
        localBackupsChip.setOnClickListener { shouldObserveLocal = true }
        installedAppsChip.setOnClickListener { shouldObserveLocal = false }
        shouldObserveLocal = !installedAppsChip.isChecked
    }

    private fun FragmentSearchBinding.startObserving(shouldObserveLocal: Boolean) {
        mainViewModel.searchInput.removeObservers(viewLifecycleOwner)
        mainViewModel.searchInput.observe(viewLifecycleOwner) { searchInput ->
            Log.d("Search", "Search input = $searchInput")
            if (searchInput == null) return@observe
            updateViewsWithSearchInput(searchInput, shouldObserveLocal)
        }
    }

    private fun FragmentSearchBinding.updateViewsWithSearchInput(
        searchInput: String,
        shouldObserveLocal: Boolean
    ) {
        launchOnViewLifecycle {
            mainViewModel.findAppsByName(searchInput, shouldObserveLocal).collect { searchResult ->
                Log.d("Search", "Search result = $searchResult")
                adapter.submitList(searchResult)
                launchPostDelayed(150L) {
                    pleaseSearchLabel.isVisible = searchResult.isEmpty()
                    imageLayout.isVisible = searchResult.isEmpty()
                }
            }
        }
    }

    private fun FragmentSearchBinding.setCheckedChip() {
        val list = searchChipGroup.children.map { it as Chip }.toList()
        val checkedChip = list[searchViewModel.checkedChipPosition]
        checkedChip.isChecked = true
    }

    override fun FragmentSearchBinding.saveRecyclerViewState() {
        searchRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            searchViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentSearchBinding.restoreRecyclerViewState() {
        searchRecyclerView.restoreRecyclerViewState(searchViewModel.savedRecyclerViewState)
    }

    override fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter =
        SearchAdapter(mainViewModel.selectionList, mainViewModel.setSelectionMode, onClickListener)

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed SearchFragment views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed SearchFragment completely")
    }
}