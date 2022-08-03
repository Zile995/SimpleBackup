package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.ui.adapters.ViewPagerAdapter
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.ViewReferenceCleaner
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.utils.extensions.findFragmentByClass
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.viewBinding
import kotlinx.coroutines.delay
import java.lang.reflect.ParameterizedType

abstract class BaseViewPagerFragment<VB : ViewBinding> : Fragment(),
    ViewReferenceCleaner {
    protected val binding by viewBinding()
    protected val mainViewModel: MainViewModel by activityViewModels()

    private var _viewPager: ViewPager2? = null
    private val viewPager get() = _viewPager!!
    private var _tabLayout: TabLayout? = null
    private val tabLayout get() = _tabLayout!!
    private var mediator: TabLayoutMediator? = null

    private val cachedPageChangeCallback by lazy {
        getOnPageChangeCallback()
    }

    private val tabPositions by lazy {
        val tabPositions = mutableListOf<Int>()
        for (position in 0 until tabLayout.tabCount) {
            tabPositions.add(position)
        }
        tabPositions
    }

    abstract fun createFragments(): ArrayList<BaseFragment<*>>
    abstract fun configureTabText(): ArrayList<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews()
        attachMediator()
        initObservers()
    }

    private fun initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.isSelected.collect { isInSelectionMode ->
                    controlTabs(shouldEnableTabs = !isInSelectionMode)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun bindViews() {
        val vbClass =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        val declaredFields = vbClass.declaredFields
        declaredFields.forEach { declaredField ->
            when (declaredField.type) {
                ViewPager2::class.java -> _viewPager =
                    vbClass.getDeclaredField(declaredField.name).get(binding) as ViewPager2
                TabLayout::class.java -> _tabLayout =
                    vbClass.getDeclaredField(declaredField.name).get(binding) as TabLayout
            }
        }
        setupViewPager()
    }

    fun shouldMoveFragmentUp() =
        childFragmentManager.findFragmentByClass<BaseFragment<*>>()?.run {
            shouldMoveFragmentUp()
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
                    launchOnViewLifecycle {
                        delay(100L)
                        stopProgressBarSpinning()
                    }
                }
            }
        }

    private fun setupViewPager() {
        viewPager.adapter = ViewPagerAdapter(
            createFragments(),
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )
        registerViewPagerCallbacks(cachedPageChangeCallback)
    }

    private fun controlTabs(shouldEnableTabs: Boolean) {
        // Have to doOnPreDraw because the selectedTabPosition update is slow on configuration change
        binding.root.doOnPreDraw {
            tabPositions.filter { position ->
                position != tabLayout.selectedTabPosition
            }.forEach { notSelectedPosition ->
                (tabLayout.getChildAt(0) as ViewGroup).getChildAt(notSelectedPosition)
                    .isEnabled = shouldEnableTabs
            }
            viewPager.isUserInputEnabled = shouldEnableTabs
        }
    }

    private fun registerViewPagerCallbacks(
        callback: ViewPager2.OnPageChangeCallback
    ) = viewPager.registerOnPageChangeCallback(callback)

    private fun unregisterViewPagerCallbacks(
        callback: ViewPager2.OnPageChangeCallback
    ) = viewPager.unregisterOnPageChangeCallback(callback)

    private fun stopProgressBarSpinning() {
        childFragmentManager.findFragmentByClass<FavoritesFragment>()?.apply {
            stopProgressBarSpinning()
        }
    }

    private fun attachMediator() {
        val tabsText = configureTabText()
        mediator = TabLayoutMediator(
            tabLayout, viewPager,
            true,
            true
        ) { tab, position ->
            if (position <= tabsText.size - 1)
                tab.text = tabsText[position]
            else tab.text = "Text is missing"
        }
        mediator!!.attach()
    }

    override fun onCleanUp() {
        unregisterViewPagerCallbacks(cachedPageChangeCallback)
        viewPager.adapter = null
        _tabLayout = null
        _viewPager = null
        mediator?.detach()
        mediator = null
    }
}