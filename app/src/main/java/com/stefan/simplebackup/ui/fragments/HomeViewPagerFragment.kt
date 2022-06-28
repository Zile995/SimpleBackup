package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.FragmentHomeViewPagerBinding
import com.stefan.simplebackup.ui.adapters.ViewPagerAdapter
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.viewmodels.HomeViewModelFactory
import com.stefan.simplebackup.utils.extensions.getResourceString
import com.stefan.simplebackup.utils.extensions.reduceDragSensitivity
import kotlinx.coroutines.launch

class HomeViewPagerFragment : BaseViewPagerFragment<FragmentHomeViewPagerBinding>() {
    // ViewModel
    private val homeViewModel: HomeViewModel by activityViewModels {
        HomeViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bindViews()
    }

    private fun FragmentHomeViewPagerBinding.bindViews() {
        viewLifecycleOwner.lifecycleScope.launch {
            setHomeAdapter()
            controlTabLayout()
        }
    }

    private fun FragmentHomeViewPagerBinding.setHomeAdapter() {
        homeViewPager.adapter = ViewPagerAdapter(
            arrayListOf(
                HomeFragment(),
                FavoritesFragment()
            ),
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )
    }

    private fun FragmentHomeViewPagerBinding.controlTabLayout() {
        homeViewPager.reduceDragSensitivity()
        mediator = TabLayoutMediator(
            binding.homeTabLayout, homeViewPager,
            true,
            true
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = requireContext().getResourceString(R.string.applications)
                }
                1 -> {
                    tab.text = requireContext().getResourceString(R.string.favorites)
                }
            }
        }
        mediator?.attach()
    }

    override fun onCleanUp() {
        super.onCleanUp()
        binding.homeViewPager.adapter = null
    }
}