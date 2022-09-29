package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentHomeViewPagerBinding
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.HomeFragment
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory

class HomeViewPagerFragment : BaseViewPagerFragment<FragmentHomeViewPagerBinding>() {
    private val homeViewModel: HomeViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel
    }

    override fun onCreateFragments(): ArrayList<BaseFragment<*>> =
        arrayListOf(HomeFragment(), FavoritesFragment.newInstance(AppDataType.USER))

    override fun onConfigureTabText(): ArrayList<String> =
        arrayListOf(
            requireContext().applicationContext.getString(R.string.applications),
            requireContext().applicationContext.getString(R.string.favorites),
            "New Favorites"
        )
}
