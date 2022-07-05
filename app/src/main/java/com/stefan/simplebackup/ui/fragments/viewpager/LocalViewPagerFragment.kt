package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
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

class LocalViewPagerFragment : BaseViewPagerFragment<FragmentLocalViewPagerBinding>() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private val localViewModel: LocalViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel.repository
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localViewModel
    }

    override fun setupViewPager(callback: () -> Unit) {
        super.setupViewPager(callback)
        binding.localViewPager.apply {
            adapter = ViewPagerAdapter(
                arrayListOf(
                    LocalFragment(),
                    FavoritesFragment.newInstance()
                ),
                childFragmentManager,
                viewLifecycleOwner.lifecycle
            )
        }
    }

    override fun FragmentLocalViewPagerBinding.provideTabLayoutMediator(): TabLayoutMediator =
        TabLayoutMediator(
            binding.localTabLayout, localViewPager,
            true,
            true
        ) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = requireContext().applicationContext.getString(R.string.backups)
                }
                1 -> {
                    tab.text = requireContext().applicationContext.getString(R.string.favorites)
                }
            }
        }

    override fun FragmentLocalViewPagerBinding.registerViewPagerCallbacks(
        callback: ViewPager2.OnPageChangeCallback
    ) = localViewPager.registerOnPageChangeCallback(callback)

    override fun FragmentLocalViewPagerBinding.unregisterViewPagerCallbacks(
        callback: ViewPager2.OnPageChangeCallback
    ) = localViewPager.unregisterOnPageChangeCallback(callback)
}