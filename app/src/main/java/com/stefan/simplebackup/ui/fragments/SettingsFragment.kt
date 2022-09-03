package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentSettingsBinding
import com.stefan.simplebackup.ui.viewmodels.SettingsViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.viewBinding

class SettingsFragment : Fragment() {
    private val binding by viewBinding(FragmentSettingsBinding::inflate)

    private val settingsViewModel: SettingsViewModel by viewModels {
        ViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            bindViews()
        }
    }

    private fun FragmentSettingsBinding.bindViews() {
        bindAppCacheSwitch()
        bindCompressionSlider()
    }

    private fun FragmentSettingsBinding.bindCompressionSlider() {
        zipCompressionSlider.value = PreferenceHelper.savedZipCompressionLevel
        zipCompressionSlider.addOnChangeListener { _, value, _ ->
            settingsViewModel.saveZipCompressionLevel(value)
        }
    }

    private fun FragmentSettingsBinding.bindAppCacheSwitch() {
        appCacheSwitch.isChecked = PreferenceHelper.shouldExcludeAppsCache
        appCacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setExcludeAppsCache(isChecked)
        }
    }
}