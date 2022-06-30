package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.databinding.FragmentCloudViewPagerBinding

class CloudViewPagerFragment : BaseViewPagerFragment<FragmentCloudViewPagerBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCleanUp() {
        super.onCleanUp()
    }

    override fun FragmentCloudViewPagerBinding.setAdapter() {
        TODO("Not yet implemented")
    }

    override fun FragmentCloudViewPagerBinding.provideTabLayoutMediator(): TabLayoutMediator {
        TODO("Not yet implemented")
    }
}