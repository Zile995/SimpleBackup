package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.stefan.simplebackup.databinding.FragmentSearchBinding
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import com.stefan.simplebackup.utils.extensions.viewBinding

class SearchFragment : Fragment(), ViewReferenceCleaner {
    private val binding: FragmentSearchBinding by viewBinding(FragmentSearchBinding::inflate) {
        onCleanUp()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onMainActivityCallback {
            openSearchView()
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                onSupportNavigateUp()
            }
        }
    }

    override fun onCleanUp() {
        return
    }
}