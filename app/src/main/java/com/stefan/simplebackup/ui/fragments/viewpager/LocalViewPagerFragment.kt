package com.stefan.simplebackup.ui.fragments.viewpager

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentLocalViewPagerBinding
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.LocalFragment
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import kotlin.properties.Delegates

class LocalViewPagerFragment : BaseViewPagerFragment<FragmentLocalViewPagerBinding>() {
    private val localViewModel: LocalViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel.repository
        )
    }

    private var isStoragePermissionGranted by Delegates.observable<Boolean?>(null) { _, _, isGranted ->
        Log.d("PermissionStatus", "Is observable granted = $isGranted")
        controlViewsOnPermissionChange(isGranted)
        isGranted?.let {
            if (isGranted) {
                addFragments(onCreateFragments())
            } else {
                removeAllFragments()
            }
        }
    }

    private val storagePermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("PermissionStatus", "First time status = $isGranted")
            isStoragePermissionGranted = isGranted
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localViewModel
        onMainActivityCallback {
            requestStoragePermission(permissionLauncher = storagePermissionLauncher,
            onPermissionAlreadyGranted = {
                isStoragePermissionGranted = true
            })
        }
    }

    override fun onStart() {
        super.onStart()
        val appPermissionManager = AppPermissionManager(requireContext().applicationContext)
        isStoragePermissionGranted =
            appPermissionManager.checkMainPermission(MainPermission.MANAGE_ALL_FILES)
    }

    private fun controlViewsOnPermissionChange(isGranted: Boolean?) {
        binding.apply {
            localViewPager.isVisible = isGranted == true
            storagePermissionLabel.isVisible = isGranted == false
        }
    }

    override fun onCreateFragments(): ArrayList<BaseFragment<*>> = arrayListOf(
        LocalFragment(),
        FavoritesFragment.newInstance(AppDataType.LOCAL)
    )

    override fun onConfigureTabText(): ArrayList<String> =
        arrayListOf(
            requireContext().applicationContext.getString(R.string.backups),
            requireContext().applicationContext.getString(R.string.favorites)
        )
}

