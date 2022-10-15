package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.FragmentSettingsBinding
import com.stefan.simplebackup.ui.viewmodels.SettingsViewModel
import com.stefan.simplebackup.ui.viewmodels.SettingsViewModelFactory
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.openAppNotificationSettings
import com.stefan.simplebackup.utils.extensions.openStorageSettings
import com.stefan.simplebackup.utils.extensions.viewBinding

class SettingsFragment : Fragment() {
    private val binding by viewBinding(FragmentSettingsBinding::inflate)

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(
            application = requireActivity().application as MainApplication
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bindViews()
    }

    override fun onResume() {
        super.onResume()
        if (getStorageInfoText() != binding.storageInfoLabel.text)
            binding.bindStorageInfoLabel()
    }

    private fun FragmentSettingsBinding.bindViews() {
        bindAppCacheSwitch()
        bindStorageInfoField()
        bindStorageInfoLabel()
        bindNotificationField()
        bindCompressionSlider()
        bindDoubleBackPressSwitch()
    }

    private fun FragmentSettingsBinding.bindAppCacheSwitch() {
        appCacheSwitch.isChecked = PreferenceHelper.shouldExcludeAppsCache
        appCacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setExcludeAppsCache(isChecked)
        }
    }

    private fun FragmentSettingsBinding.bindStorageInfoField() {
        storageInfoField.setOnClickListener {
            requireContext().openStorageSettings()
        }
    }

    private fun FragmentSettingsBinding.bindStorageInfoLabel() {
        storageInfoLabel.text = getStorageInfoText()
    }

    private fun getStorageInfoText() =
        getString(
            R.string.storage_info, settingsViewModel.getUsedStorage() / 1000.0,
            settingsViewModel.getTotalStorage() / 1000.0
        )

    private fun FragmentSettingsBinding.bindNotificationField() {
        notificationSettingsField.setOnClickListener {
            requireContext().openAppNotificationSettings()
        }
    }

    private fun FragmentSettingsBinding.bindDoubleBackPressSwitch() {
        doublePressSwitch.isChecked = PreferenceHelper.shouldDoublePressToExit
        doublePressSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setDoublePressBackToExit(isChecked)
        }
    }

    private fun FragmentSettingsBinding.bindCompressionSlider() {
        zipCompressionSlider.value = PreferenceHelper.savedZipCompressionLevel
        zipCompressionSlider.addOnChangeListener { _, value, _ ->
            settingsViewModel.saveZipCompressionLevel(value)
        }
    }
}