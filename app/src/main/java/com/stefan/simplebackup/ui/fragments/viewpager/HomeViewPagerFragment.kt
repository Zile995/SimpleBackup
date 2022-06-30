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
import com.stefan.simplebackup.utils.extensions.reduceDragSensitivity
import com.stefan.simplebackup.utils.extensions.viewModel
import kotlinx.coroutines.launch

class HomeViewPagerFragment : BaseViewPagerFragment<FragmentHomeViewPagerBinding>() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModel {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel
        )
    }

    private val fragmentList by lazy {
        arrayListOf(
            HomeFragment(),
            FavoritesFragment()
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
            setTabLayoutMediator()
        }
    }

    override fun FragmentHomeViewPagerBinding.setAdapter() {
        homeViewPager.adapter = ViewPagerAdapter(
            fragmentList,
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )
    }

    override fun FragmentHomeViewPagerBinding.setTabLayoutMediator() {
        homeViewPager.reduceDragSensitivity()
        mediator = TabLayoutMediator(
            binding.homeTabLayout, homeViewPager,
            true,
            true
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = context?.applicationContext?.getString(R.string.applications)
                }
                1 -> {
                    tab.text = context?.applicationContext?.getString(R.string.favorites)
                }
            }
        }
        mediator?.attach()
    }

    override fun onCleanUp() {
        binding.homeViewPager.adapter = null
        super.onCleanUp()
    }
}