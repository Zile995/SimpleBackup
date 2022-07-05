package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.FragmentHomeViewPagerBinding
import com.stefan.simplebackup.ui.adapters.ViewPagerAdapter
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.HomeFragment
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory

class HomeViewPagerFragment : BaseViewPagerFragment<FragmentHomeViewPagerBinding>() {
    private val mainViewModel: MainViewModel by activityViewModels()
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

    override fun setupViewPager(callback: () -> Unit) {
        super.setupViewPager(callback)
        binding.homeViewPager.apply {
            adapter = ViewPagerAdapter(
                arrayListOf(
                    HomeFragment(),
                    FavoritesFragment.newInstance()
                ),
                childFragmentManager,
                viewLifecycleOwner.lifecycle
            )
        }
    }

    override fun FragmentHomeViewPagerBinding.provideTabLayoutMediator(): TabLayoutMediator =
        TabLayoutMediator(
            homeTabLayout, homeViewPager,
            true,
            true
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = requireContext().applicationContext.getString(R.string.applications)
                }
                1 -> {
                    tab.text = requireContext().applicationContext.getString(R.string.favorites)
                }
            }
        }

    override fun FragmentHomeViewPagerBinding.registerViewPagerCallbacks(
        callback: ViewPager2.OnPageChangeCallback
    ) = homeViewPager.registerOnPageChangeCallback(callback)

    override fun FragmentHomeViewPagerBinding.unregisterViewPagerCallbacks(
        callback: ViewPager2.OnPageChangeCallback
    ) = homeViewPager.unregisterOnPageChangeCallback(callback)
}
