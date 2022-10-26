package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import com.stefan.simplebackup.utils.extensions.onMainActivity
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import kotlinx.coroutines.delay

class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private val searchViewModel: SearchViewModel by viewModels {
        SearchViewModelFactory()
    }

    init {
        shouldEnableOnLongClick = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnBackPressedCallback()
        binding.apply {
            bindChipGroup()
            initObservers()
        }
    }

    private fun setOnBackPressedCallback() {
        onMainActivity {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                onSupportNavigateUp()
            }
        }
    }

    private fun FragmentSearchBinding.bindChipGroup() {
        searchChipGroup.children.forEach { chipView ->
            val chip = chipView as Chip
            chip.setOnCheckedChangeListener { buttonView, _ ->
                val index = searchChipGroup.indexOfChild(buttonView)
                searchChipGroup.removeView(buttonView)
                searchChipGroup.addView(buttonView, index)
            }
        }
    }

    private fun FragmentSearchBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.searchResult.collect { searchResults ->
                    Log.d("Search", "Search result = ${searchResults.map { it.name }}")
                    adapter.submitList(searchResults)
                    if (searchResults.isEmpty()) delay(150L)
                    pleaseSearchLabel.isVisible = searchResults.isEmpty()
                    imageLayout.isVisible = searchResults.isEmpty()
                }
            }
        }
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