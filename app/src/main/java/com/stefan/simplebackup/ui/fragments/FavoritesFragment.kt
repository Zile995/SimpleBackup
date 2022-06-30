package com.stefan.simplebackup.ui.fragments

import android.util.Log
import com.stefan.simplebackup.databinding.FragmentFavoritesBinding
import com.stefan.simplebackup.ui.adapters.FavoritesAdapter
import com.stefan.simplebackup.utils.extensions.onRestoreRecyclerViewState
import com.stefan.simplebackup.utils.extensions.onSaveRecyclerViewState
import java.lang.ref.WeakReference

class FavoritesFragment :
    BaseFragment<FragmentFavoritesBinding>() {
    private var _favoritesAdapter: FavoritesAdapter? = null
    private val favoritesAdapter get() = _favoritesAdapter!!

    override fun onCleanUp() {
        _favoritesAdapter = null
    }

    override fun WeakReference<FragmentFavoritesBinding>.saveRecyclerViewState() {
        val binding = this.get()
        binding?.apply {
            favoritesRecyclerView.onSaveRecyclerViewState { stateParcelable ->
                //favoritesViewModel.saveRecyclerViewState(stateParcelable)
            }
        }
    }

    override fun FragmentFavoritesBinding.restoreRecyclerViewState() {
        favoritesRecyclerView.onRestoreRecyclerViewState(null)
    }

    override fun onDestroyView() {
        Log.d("FavoritesFragment", "Destroyed FavoritesFragment Views")
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FavoritesFragment", "Destroyed FavoritesFragment completely")
    }

}

