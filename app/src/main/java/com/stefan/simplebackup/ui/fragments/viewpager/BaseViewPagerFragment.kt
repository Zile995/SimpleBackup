package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.ViewReferenceCleaner
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.viewBinding

abstract class BaseViewPagerFragment<VB : ViewBinding> : Fragment(), ViewPagerProvider<VB>,
    ViewReferenceCleaner {
    protected val binding by viewBinding()
    private var mediator: TabLayoutMediator? = null
    protected val mainViewModel: MainViewModel by activityViewModels()

    private val cachedPageChangeCallback by lazy {
        getOnPageChangeCallback()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            setupViewPager {
                registerViewPagerCallbacks(cachedPageChangeCallback)
            }
            attachMediator()
            initObservers()
        }
    }

    override fun setupTabLayout(callback: () -> Unit) {
        callback()
    }

    override fun setupViewPager(callback: () -> Unit) {
        callback()
    }

    private fun VB.initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.shouldDisableTab.collect { shouldDisable ->
                    if (shouldDisable) {
                        setupTabLayout {
                            disableTab(1)
                        }
                    }
                }
            }
        }
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
                    cachedPosition != position -> true
                    else -> false
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
        binding.unregisterViewPagerCallbacks(cachedPageChangeCallback)
        mediator?.detach()
        mediator = null
    }
}