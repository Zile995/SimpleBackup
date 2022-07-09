package com.stefan.simplebackup.ui.fragments.viewpager

import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

interface ViewPagerProvider<VB : ViewBinding> {

    fun setupViewPager(callback: () -> Unit)

    fun setupTabLayout(callback: () -> Unit)

    fun VB.disableTab(position: Int)

    fun VB.attachMediator()

    fun VB.provideTabLayoutMediator(): TabLayoutMediator

    fun VB.registerViewPagerCallbacks(callback: ViewPager2.OnPageChangeCallback)

    fun VB.unregisterViewPagerCallbacks(callback: ViewPager2.OnPageChangeCallback)

}