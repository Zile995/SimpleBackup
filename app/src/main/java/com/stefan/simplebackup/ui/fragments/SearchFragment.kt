package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.chip.Chip
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentSearchBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.SearchAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.SearchViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay

class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private val searchViewModel: SearchViewModel by viewModels {
        ViewModelFactory(requireActivity().application as MainApplication)
    }

    init {
        shouldEnableOnLongClick = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnBackPressedCallback()
        binding.apply {
            bindChipGroup()
            initObservers()
        }
    }

    private fun setOnBackPressedCallback() {
        onMainActivityCallback {
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
            repeatOnViewLifecycle(Lifecycle.State.CREATED) {
                mainViewModel.searchResult.collect { searchResults ->
                    Log.d("Search", "Search result = ${searchResults.map { it.name }}")
                    adapter.submitList(searchResults)
                    if (searchResults.isEmpty()) delay(150)
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
        SearchAdapter(
            mainViewModel.selectionList,
            mainViewModel.setSelectionMode
        ) { onClickListener }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed SearchFragment views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed SearchFragment completely")
    }
}