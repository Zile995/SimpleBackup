package com.stefan.simplebackup.utils.extensions

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

inline fun AppCompatActivity.requestPermission(
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