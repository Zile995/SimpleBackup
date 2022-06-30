package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.FragmentLocalViewPagerBinding
import com.stefan.simplebackup.ui.adapters.ViewPagerAdapter
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.LocalFragment
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.reduceDragSensitivity
import com.stefan.simplebackup.utils.extensions.viewModel
import kotlinx.coroutines.launch

class LocalViewPagerFragment : BaseViewPagerFragment<FragmentLocalViewPagerBinding>() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private val localViewModel: LocalViewModel by viewModel {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel.repository
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localViewModel
        binding.bindViews()
    }

    private fun FragmentLocalViewPagerBinding.bindViews() {
        viewLifecycleOwner.lifecycleScope.launch {
            setAdapter()
            setTabLayoutMediator()
        }
    }

    override fun FragmentLocalViewPagerBinding.setAdapter() {
        localViewPager.adapter = ViewPagerAdapter(
            arrayListOf(
                LocalFragment(),
                FavoritesFragment()
            ),
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )
    }

    override fun FragmentLocalViewPagerBinding.setTabLayoutMediator() {
        localViewPager.reduceDragSensitivity()
        mediator = TabLayoutMediator(
            binding.localTabLayout, localViewPager,
            true,
            true
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = context?.applicationContext?.getString(R.string.backups)
                }
                1 -> {
                    tab.text = context?.applicationContext?.getString(R.string.favorites)
                }
            }
        }
        mediator?.attach()
    }

    override fun onCleanUp() {
        binding.localViewPager.adapter = null
        super.onCleanUp()
    }
}