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
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.onMainActivity
import com.stefan.simplebackup.utils.extensions.viewBinding
import kotlinx.coroutines.delay

class ConfigureSheetFragment : BottomSheetDialogFragment() {

    private val binding by viewBinding(FragmentConfigureSheetBinding::inflate)
    private val mainViewModel: MainViewModel by activityViewModels()

    private var cloudBackupClicked = false

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                onMainActivity { requestSignIn() }
            } else {
                onMainActivity { showContactsPermissionDialog() }
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (!cloudBackupClicked)
                    startProgressActivity()
            } else {
                onMainActivity { showStoragePermissionDialog() }
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
            onMainActivity {
                requestStoragePermission(storagePermissionLauncher,
                    onPermissionAlreadyGranted = {
                        startProgressActivity()
                    })
            }
        }
    }

    private fun startProgressActivity() = onMainActivity {
        launchOnViewLifecycle {
            startProgressActivity(mainViewModel.selectionList.toTypedArray(), AppDataType.USER)
            mainViewModel.setSelectionMode(false)
            dismiss()
        }
    }

    private fun FragmentConfigureSheetBinding.bindCloudButton() {
        cloudBackupButton.setOnClickListener {
            cloudBackupClicked = true
            onMainActivity {
                proceedWithPermission(MainPermission.MANAGE_ALL_FILES,
                    onPermissionGranted = {
                        requestContactsPermission(contactsPermissionLauncher,
                            onPermissionAlreadyGranted = {
                                requestSignIn()
                            })
                    }, onPermissionDenied = {
                        requestStoragePermission(storagePermissionLauncher)
                    })
            }
        }
    }
}