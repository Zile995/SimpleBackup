package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentFavoritesBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.FavoritesAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.FavoritesViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*

class FavoritesFragment : BaseFragment<FragmentFavoritesBinding>() {
    private val homeViewModel: FavoritesViewModel by viewModels {
        val appDataType = getEnumExtra<AppDataType>()
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel.repository,
            appDataType
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            initObservers()
            restoreRecyclerViewState()
        }
    }

    override fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter =
        FavoritesAdapter(
            mainViewModel.selectionList,
            mainViewModel.setSelectionMode
        ) {
            onClickListener
        }

    private fun FragmentFavoritesBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        homeViewModel.observableList.collect { appList ->
                            adapter.submitList(appList)
                        }
                }
            }
        }
    }

    fun stopProgressBarSpinning() =
        homeViewModel.setSpinning(shouldSpin = false)

    override fun FragmentFavoritesBinding.saveRecyclerViewState() {
        favoritesRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            homeViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentFavoritesBinding.restoreRecyclerViewState() {
        favoritesRecyclerView.restoreRecyclerViewState(homeViewModel.savedRecyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed FavoritesFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed FavoritesFragment completely")
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         */
        fun newInstance(appDataType: AppDataType) =
            FavoritesFragment().apply {
                putEnumExtra(appDataType)
            }
    }

}

