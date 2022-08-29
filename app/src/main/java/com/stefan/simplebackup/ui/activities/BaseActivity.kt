package com.stefan.simplebackup.ui.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stefan.simplebackup.R

abstract class BaseActivity : AppCompatActivity() {

    // TODO: Handle all shared permissions requests here
    private var isGranted = false

    protected val requestPermissionLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            this.isGranted = isGranted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.background)
    }

    protected inline fun requestPermission(
        permission: String,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        continuationCallBack: () -> Unit,
        dialogCallBack: () -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                continuationCallBack()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                dialogCallBack()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

}