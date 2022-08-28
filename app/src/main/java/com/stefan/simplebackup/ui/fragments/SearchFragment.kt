package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentSearchBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.adapters.SearchAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.SearchViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import kotlinx.coroutines.launch

class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    private var _searchAdapter: SearchAdapter? = null
    private val searchAdapter get() = _searchAdapter!!

    private val searchViewModel: SearchViewModel by viewModels {
        ViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnBackPressedCallback()
        binding.bindChipGroup()
        searchViewModel
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

    override fun FragmentSearchBinding.saveRecyclerViewState() {
        searchRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            searchViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentSearchBinding.restoreRecyclerViewState() {
        searchRecyclerView.restoreRecyclerViewState(searchViewModel.savedRecyclerViewState)
    }

    override fun MainRecyclerView.setMainAdapter() {
        _searchAdapter = SearchAdapter(
            mainViewModel.selectionList,
            mainViewModel.setSelectionMode
        ) {
            object : OnClickListener {
                override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = searchAdapter.currentList[position]
                    if (searchAdapter.hasSelectedItems()) {
                        searchAdapter.doSelection(holder as BaseViewHolder, item)
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
                    val item = searchAdapter.currentList[position]
                    mainViewModel.setSelectionMode(true)
                    searchAdapter.doSelection(holder as BaseViewHolder, item)
                }
            }
        }
        adapter = searchAdapter
    }

    override fun onCleanUp() {
        super.onCleanUp()
        _searchAdapter = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed SearchFragment views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed SearchFragment completely")
    }

}