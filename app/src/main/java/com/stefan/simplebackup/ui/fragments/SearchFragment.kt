package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.stefan.simplebackup.databinding.FragmentSearchBinding
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import com.stefan.simplebackup.utils.extensions.onViewLifecycleScope
import com.stefan.simplebackup.utils.extensions.viewBinding
import kotlinx.coroutines.launch

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
            requestFocus()
            onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onViewLifecycleScope {
                        launch {
                            revertToolBarToInitialSize()
                        }
                        findNavController().popBackStack()
                    }
                }
            })
        }
    }

    override fun onCleanUp() {
        return
    }
}