package com.stefan.simplebackup.ui.fragments

import com.stefan.simplebackup.databinding.FragmentLocalViewPagerBinding

class LocalViewPagerFragment : BaseViewPagerFragment<FragmentLocalViewPagerBinding>() {

    override fun onCleanUp() {
        super.onCleanUp()
        binding.localViewPager.adapter = null
    }
}