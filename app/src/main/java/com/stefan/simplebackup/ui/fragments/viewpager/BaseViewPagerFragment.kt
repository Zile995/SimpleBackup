package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.ViewReferenceCleaner
import com.stefan.simplebackup.utils.extensions.viewBinding


abstract class BaseViewPagerFragment<VB : ViewBinding> : Fragment(), MediatorProvider<VB>,
    ViewReferenceCleaner {
    protected val binding by viewBinding()
    private var mediator: TabLayoutMediator? = null

    private var cachedPageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            setupViewPager {
                cachedPageChangeCallback = getOnPageChangeCallback()
                registerViewPagerCallbacks(cachedPageChangeCallback!!)
            }
            attachMediator()
        }
    }

    override fun setupViewPager(callback: () -> Unit) {
        callback()
    }

    private fun getOnPageChangeCallback(): ViewPager2.OnPageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            var shouldStopSpinning = false
            var cachedPosition: Int = 0

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                shouldStopSpinning = when {
                    cachedPosition != position -> {
                        true
                    }
                    else -> {
                        false
                    }
                }
                cachedPosition = position
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE && shouldStopSpinning) {
                    stopProgressBarSpinning()
                }
            }
        }

    private fun stopProgressBarSpinning() {
        childFragmentManager.fragments.forEach { childFragment ->
            if (childFragment is FavoritesFragment) childFragment.stopProgressBarSpinning
        }
    }

    override fun VB.attachMediator() {
        mediator = provideTabLayoutMediator()
        mediator!!.attach()
    }

    override fun onCleanUp() {
        binding.unregisterViewPagerCallbacks(cachedPageChangeCallback!!)
        cachedPageChangeCallback = null
        mediator?.detach()
        mediator = null
    }
}