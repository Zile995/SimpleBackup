package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentConfigureSheetBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import com.stefan.simplebackup.utils.extensions.viewBinding

class ConfigureSheetFragment : BottomSheetDialogFragment() {

    private val binding by viewBinding(FragmentConfigureSheetBinding::inflate)
    private val mainViewModel: MainViewModel by activityViewModels()

    private var cloudBackupClicked = false

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (cloudBackupClicked)
                    startProgressActivity(AppDataType.CLOUD)
            } else {
                onShowDialog { showContactsPermissionDialog() }
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (!cloudBackupClicked)
                    startProgressActivity(AppDataType.LOCAL)
            } else {
                onShowDialog { showStoragePermissionDialog() }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            bindViews()
        }
    }

    private fun FragmentConfigureSheetBinding.bindViews() {
        bindLocalButton()
        bindCloudButton()
    }

    private fun FragmentConfigureSheetBinding.bindLocalButton() {
        localBackupButton.setOnClickListener {
            cloudBackupClicked = false
            onMainActivityCallback {
                requestStoragePermission(storagePermissionLauncher, onPermissionAlreadyGranted = {
                    startProgressActivity(AppDataType.LOCAL)
                })
            }
        }
    }

    private inline fun onShowDialog(crossinline dialog: MainActivity.() -> Unit) =
        onMainActivityCallback(dialog)

    private fun startProgressActivity(appDataType: AppDataType) = onMainActivityCallback {
        startProgressActivity(mainViewModel.selectionList.toTypedArray(), appDataType)
    }

    private fun FragmentConfigureSheetBinding.bindCloudButton() {
        cloudBackupButton.setOnClickListener {
            cloudBackupClicked = true
            onMainActivityCallback {
                proceedWithPermission(MainPermission.MANAGE_ALL_FILES,
                    onPermissionGranted = {
                        requestContactsPermission(contactsPermissionLauncher,
                            onPermissionAlreadyGranted = {
                                startProgressActivity(AppDataType.CLOUD)
                            })
                    }, onPermissionDenied = {
                        requestStoragePermission(storagePermissionLauncher)
                    })
            }
        }
    }
}