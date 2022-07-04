package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.stefan.simplebackup.utils.extensions.viewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment(), RecyclerViewSaver<VB>,
    ViewReferenceCleaner {
    protected val binding by viewBinding()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onCleanUp() {
        binding.saveRecyclerViewState()
    }
}