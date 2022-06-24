package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.stefan.simplebackup.databinding.FragmentViewPagerBinding
import com.stefan.simplebackup.ui.adapters.ViewPagerAdapter
import com.stefan.simplebackup.utils.extensions.viewBinding

class ViewPagerFragment : Fragment() {
    private val binding by viewBinding(FragmentViewPagerBinding::inflate)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindViews()
        return binding.root
    }

    private fun bindViews() {
        binding.apply {
            viewPager.adapter = ViewPagerAdapter(
                arrayListOf(
                    HomeFragment(),
                    LocalFragment()
                ),
                childFragmentManager,
                viewLifecycleOwner.lifecycle
            )
        }
    }
}