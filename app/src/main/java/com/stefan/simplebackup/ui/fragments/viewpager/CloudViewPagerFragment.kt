package com.stefan.simplebackup.ui.fragments.viewpager

import android.content.Context
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
import kotlin.properties.Delegates

class CloudViewPagerFragment : BaseViewPagerFragment<FragmentCloudViewPagerBinding>() {
    private val homeViewModel: HomeViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel
        )
    }

    private var isContactsPermissionGranted by Delegates.observable(false) { _, _, isGranted ->
        controlViewsOnPermissionChange(isGranted)
        if (isGranted) {
            val fragmentList = arrayListOf(
                CloudFragment(),
                FavoritesFragment.newInstance(AppDataType.CLOUD)
            )
            addFragments(fragmentList)
        } else removeAllFragments()
    }

    private val contactsPermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            isContactsPermissionGranted = isGranted
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onMainActivityCallback {
            requestContactsPermission(permissionLauncher = contactsPermissionLauncher)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel
    }

    override fun onStart() {
        super.onStart()
        val appPermissionManager = AppPermissionManager(requireContext().applicationContext)
        isContactsPermissionGranted =
            appPermissionManager.mainPermissionCheck(MainPermissions.CONTACTS)
    }

    private fun controlViewsOnPermissionChange(isGranted: Boolean) {
        binding.apply {
            cloudViewPager.isVisible = isGranted
            contactsPermissionLabel.isVisible = !isGranted
            controlTabs(isGranted)
        }
    }

    override fun createFragments(): ArrayList<BaseFragment<*>> = arrayListOf()

    override fun configureTabText(): ArrayList<String> =
        arrayListOf(
            requireContext().applicationContext.getString(R.string.cloud_backups),
            requireContext().applicationContext.getString(R.string.favorites)
        )
}