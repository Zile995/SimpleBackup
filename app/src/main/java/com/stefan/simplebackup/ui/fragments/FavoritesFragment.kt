package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.databinding.FragmentFavoritesBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.FavoritesAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.FavoritesViewModel
import com.stefan.simplebackup.ui.viewmodels.FavoritesViewModelFactory
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.repeatOnStarted
import kotlinx.coroutines.delay

class FavoritesFragment : BaseFragment<FragmentFavoritesBinding>() {
    private val favoritesViewModel: FavoritesViewModel by viewModels {
        FavoritesViewModelFactory().factory(mainViewModel.repository)
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
            mainViewModel.setSelectionMode,
            onClickListener
        )

    private fun FragmentFavoritesBinding.initObservers() {
        launchOnViewLifecycle {
            repeatOnStarted {
                favoritesViewModel.spinner.collect { isSpinning ->
                    progressBar.isVisible = isSpinning
                    if (!isSpinning)
                        favoritesViewModel.observableList.collect { appList ->
                            adapter.submitList(appList)
                            if (appList.isEmpty()) delay(250L)
                            noFavoritesLabel.isVisible = appList.isEmpty()
                        }
                }
            }
        }
    }

    fun stopProgressBarSpinning() =
        favoritesViewModel.setSpinning(shouldSpin = false)

    override fun FragmentFavoritesBinding.saveRecyclerViewState() {
        favoritesRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            favoritesViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentFavoritesBinding.restoreRecyclerViewState() {
        favoritesRecyclerView.restoreRecyclerViewState(favoritesViewModel.savedRecyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Fragments", "Destroyed FavoritesFragment Views")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Fragments", "Destroyed FavoritesFragment completely")
    }
}

