package com.stefan.simplebackup.ui.fragments.viewpager

import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator

interface MediatorProvider<VB : ViewBinding> {

    fun setupViewPager(callback: () -> Unit)

    fun VB.registerViewPagerCallbacks(callback: ViewPager2.OnPageChangeCallback)

    fun VB.unregisterViewPagerCallbacks(callback: ViewPager2.OnPageChangeCallback)

    fun VB.attachMediator()

    fun VB.provideTabLayoutMediator(): TabLayoutMediator

}