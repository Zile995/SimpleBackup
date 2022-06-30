package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
import com.stefan.simplebackup.utils.extensions.viewModel
import kotlinx.coroutines.launch

class HomeViewPagerFragment : BaseViewPagerFragment<FragmentHomeViewPagerBinding>() {
    // ViewModel
    private val mainViewModel: MainViewModel by activityViewModels()

    private val homeViewModel: HomeViewModel by viewModel {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel
        binding.bindViews()
    }

    private fun FragmentHomeViewPagerBinding.bindViews() {
        viewLifecycleOwner.lifecycleScope.launch {
            setAdapter()
            setTabLayoutMediator(homeViewPager) {
                provideTabLayoutMediator()
            }
        }
    }

    override fun FragmentHomeViewPagerBinding.setAdapter() {
        homeViewPager.adapter = ViewPagerAdapter(
            arrayListOf(
                HomeFragment(),
                FavoritesFragment()
            ),
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )
    }

    override fun FragmentHomeViewPagerBinding.provideTabLayoutMediator(): TabLayoutMediator =
        TabLayoutMediator(
            binding.homeTabLayout, homeViewPager,
            true,
            true
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = requireContext().getString(R.string.applications)
                }
                1 -> {
                    tab.text = requireContext().getString(R.string.favorites)
                }
            }
        }

    override fun onCleanUp() {
        super.onCleanUp()
        binding.homeViewPager.adapter = null
    }
}