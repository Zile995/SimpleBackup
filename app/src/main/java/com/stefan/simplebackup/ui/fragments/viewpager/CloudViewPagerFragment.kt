package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermissions
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentCloudViewPagerBinding
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.CloudFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.viewmodels.HomeViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback

class CloudViewPagerFragment : BaseViewPagerFragment<FragmentCloudViewPagerBinding>() {
    private val homeViewModel: HomeViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel
        )
    }

    private val contactsPermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            binding.apply {
                cloudViewPager.isVisible = isGranted
                contactsPermissionLabel.isVisible = !isGranted
                controlTabs(isGranted)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onMainActivityCallback {
            requestContactsPermission(
                permissionLauncher = contactsPermissionLauncher
            )
        }
    }

    override fun onStart() {
        super.onStart()
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        val appPermissionManager = AppPermissionManager(requireContext().applicationContext)
        val isContactsPermissionGranted =
            appPermissionManager.mainPermissionCheck(MainPermissions.CONTACTS)
        binding.cloudViewPager.isVisible = isContactsPermissionGranted
        binding.contactsPermissionLabel.isVisible = !isContactsPermissionGranted
        controlTabs(isContactsPermissionGranted)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel
    }

    override fun createFragments(): ArrayList<BaseFragment<*>> =
        arrayListOf(CloudFragment(), FavoritesFragment.newInstance(AppDataType.CLOUD))

    override fun configureTabText(): ArrayList<String> =
        arrayListOf(
            requireContext().applicationContext.getString(R.string.cloud_backups),
            requireContext().applicationContext.getString(R.string.favorites)
        )
}