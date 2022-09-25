package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stefan.simplebackup.data.manager.MainPermission
import com.stefan.simplebackup.databinding.FragmentConfigureSheetBinding
import com.stefan.simplebackup.ui.activities.ProgressActivity
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.viewmodels.SELECTION_EXTRA
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import com.stefan.simplebackup.utils.extensions.passBundleToActivity
import com.stefan.simplebackup.utils.extensions.viewBinding

class ConfigureSheetFragment : BottomSheetDialogFragment() {
    private val binding by viewBinding(FragmentConfigureSheetBinding::inflate)

    private val mainViewModel: MainViewModel by activityViewModels()

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                //
            } else {
                onMainActivityCallback {
                    showContactsPermissionDialog()
                }
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startProgressActivity()
            } else {
                onMainActivityCallback {
                    showStoragePermissionDialog()
                }
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
            onMainActivityCallback {
                requestStoragePermission(storagePermissionLauncher,
                    onPermissionAlreadyGranted = {
                        startProgressActivity()
                    })
            }
        }
    }

    private fun startProgressActivity() {
        requireContext().apply {
            passBundleToActivity<ProgressActivity>(
                SELECTION_EXTRA to mainViewModel.selectionList.toTypedArray()
            )
        }
    }

    private fun FragmentConfigureSheetBinding.bindCloudButton() {
        cloudBackupButton.setOnClickListener {
            onMainActivityCallback {
                proceedWithPermission(MainPermission.MANAGE_ALL_FILES, onPermissionGranted = {
                    requestContactsPermission(
                        contactsPermissionLauncher,
                        onPermissionAlreadyGranted = {
                            //
                        })
                }, onPermissionDenied = {
                    requestStoragePermission(storagePermissionLauncher)
                })
            }
        }
    }
}