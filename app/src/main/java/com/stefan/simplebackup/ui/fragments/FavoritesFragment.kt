package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentFavoritesBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.adapters.FavoritesAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.FavoritesViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.launch

class FavoritesFragment : BaseFragment<FragmentFavoritesBinding>() {
    private var _favoritesAdapter: FavoritesAdapter? = null
    private val favoritesAdapter get() = _favoritesAdapter!!

    private val homeViewModel: FavoritesViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel
        )
    }

    val stopProgressBarSpinning by lazy {
        homeViewModel.stopSpinning(false)
    }

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

    private fun FragmentFavoritesBinding.bindViews() {
        bindRecyclerView()
    }

    private fun FragmentFavoritesBinding.bindRecyclerView() {
        favoritesRecyclerView.apply {
            setFavoritesAdapter()
            setHasFixedSize(true)
        }
    }

    private fun RecyclerView.setFavoritesAdapter() {
        _favoritesAdapter = FavoritesAdapter(
            homeViewModel.selectionList,
            homeViewModel.setSelectionMode
        ) {
            object : OnClickListener {
                override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = favoritesAdapter.currentList[position]
                    if (favoritesAdapter.hasSelectedItems()) {
                        favoritesAdapter.doSelection(holder as BaseViewHolder, item)
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
                    val item = favoritesAdapter.currentList[position]
                    homeViewModel.setSelectionMode(true)
                    favoritesAdapter.doSelection(holder as BaseViewHolder, item)
                }
            }
        }
        adapter = favoritesAdapter
    }

    private fun FragmentFavoritesBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.isSelected.collect { isSelected ->
                        mainViewModel.changeTab(isSelected)
                        batchBackup.isVisible = isSelected
                    }
                }
                homeViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        homeViewModel.observableList.collect { appList ->
                            favoritesAdapter.submitList(appList)
                        }
                }
            }
        }
    }

    private fun FragmentFavoritesBinding.setActivityCallBacks() {
        onMainActivityCallback {
            favoritesRecyclerView.controlFloatingButton()
        }
    }

    override fun FragmentFavoritesBinding.saveRecyclerViewState() {
        favoritesRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentFavoritesBinding.restoreRecyclerViewState() {
        favoritesRecyclerView.onRestoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed FavoritesFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed FavoritesFragment completely")
    }

    override fun onCleanUp() {
        super.onCleanUp()
        _favoritesAdapter = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         */
        fun newInstance() =
            FavoritesFragment()
    }

}

