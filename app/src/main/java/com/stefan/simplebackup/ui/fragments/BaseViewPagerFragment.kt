package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.stefan.simplebackup.utils.extensions.viewBinding

abstract class BaseViewPagerFragment<VB : ViewBinding> : Fragment(), ViewReferenceCleaner {
    protected val binding by viewBinding()
    protected var mediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onCleanUp() {
        mediator?.detach()
        mediator = null
    }
}