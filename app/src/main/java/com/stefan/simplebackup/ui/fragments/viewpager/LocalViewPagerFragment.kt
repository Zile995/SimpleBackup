package com.stefan.simplebackup.ui.fragments.viewpager

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.manager.AppPermissionManager
import com.stefan.simplebackup.data.manager.MainPermissions
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

    private var isStoragePermissionGranted by Delegates.observable(false) { _, _, isGranted ->
        Log.d("PermissionStatus", "Is observable granted = $isGranted")
        controlViewsOnPermissionChange(isGranted)
        if (isGranted) {
            val fragmentList = arrayListOf(
                LocalFragment(),
                FavoritesFragment.newInstance(AppDataType.LOCAL)
            )
            addFragments(fragmentList)
        } else removeAllFragments()
    }

    private val storagePermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("PermissionStatus", "First time status = $isGranted")
            isStoragePermissionGranted = isGranted
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onMainActivityCallback {
            requestStoragePermission(permissionLauncher = storagePermissionLauncher)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localViewModel
    }

    override fun onStart() {
        super.onStart()
        val appPermissionManager = AppPermissionManager(requireContext().applicationContext)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            isStoragePermissionGranted =
                appPermissionManager.mainPermissionCheck(MainPermissions.STORAGE)
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            isStoragePermissionGranted = appPermissionManager.checkManageAllFilesPermission()
        }
    }

    private fun controlViewsOnPermissionChange(isGranted: Boolean) {
        binding.apply {
            localViewPager.isVisible = isGranted
            storagePermissionLabel.isVisible = !isGranted
            controlTabs(isGranted)
        }
    }

    override fun createFragments(): ArrayList<BaseFragment<*>> = arrayListOf()

    override fun configureTabText(): ArrayList<String> =
        arrayListOf(
            requireContext().applicationContext.getString(R.string.backups),
            requireContext().applicationContext.getString(R.string.favorites)
        )
}

