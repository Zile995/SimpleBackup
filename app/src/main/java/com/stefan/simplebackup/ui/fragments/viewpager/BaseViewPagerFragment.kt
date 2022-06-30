package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.databinding.FragmentHomeViewPagerBinding
import com.stefan.simplebackup.ui.fragments.ViewReferenceCleaner
import com.stefan.simplebackup.utils.extensions.reduceDragSensitivity
import com.stefan.simplebackup.utils.extensions.viewBinding

abstract class BaseViewPagerFragment<VB : ViewBinding> : Fragment(), ViewReferenceCleaner {
    protected val binding by viewBinding()
    private var mediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    abstract fun VB.setAdapter()
    abstract fun VB.provideTabLayoutMediator(): TabLayoutMediator

    protected fun setTabLayoutMediator(
        viewPager: ViewPager2,
        mediatorFactory: () -> TabLayoutMediator
    ) {
        viewPager.reduceDragSensitivity()
        mediator = mediatorFactory()
        mediator?.attach()
    }

    override fun onCleanUp() {
        mediator?.detach()
        mediator = null
    }
}