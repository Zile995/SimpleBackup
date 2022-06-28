package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.stefan.simplebackup.databinding.FragmentCloudBinding
import com.stefan.simplebackup.ui.adapters.CloudAdapter
import com.stefan.simplebackup.utils.extensions.viewBinding
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class CloudFragment : BaseFragment<FragmentCloudBinding>() {
    private var _cloudAdapter: CloudAdapter? = null
    private val cloudAdapter get() = _cloudAdapter!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            bindViews()
        }
    }

    private fun FragmentCloudBinding.bindViews() {
        viewLifecycleOwner.lifecycleScope.launch {

        }
    }

    override fun onCleanUp() {
        _cloudAdapter = null
    }

    override fun WeakReference<FragmentCloudBinding>.saveRecyclerViewState() {
        TODO("Not yet implemented")
    }

    override fun FragmentCloudBinding.restoreRecyclerViewState() {
        TODO("Not yet implemented")
    }
}