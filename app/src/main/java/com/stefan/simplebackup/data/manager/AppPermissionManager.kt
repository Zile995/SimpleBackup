package com.stefan.simplebackup.data.manager

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class AppPermissionManager(private val context: Context) {

    private val appOpsService by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    @Suppress("DEPRECATION")
    fun checkUsageStatsPermission(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appOpsService.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        else
            appOpsService.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )

        return if (mode == AppOpsManager.MODE_DEFAULT)
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        else mode == AppOpsManager.MODE_ALLOWED
    }

    fun checkManageAllFilesPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val mode = AppOpsManager.permissionToOp(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            ?.let { appOpName ->
                appOpsService.unsafeCheckOpNoThrow(
                    appOpName,
                    Process.myUid(),
                    context.packageName
                )
            }

        return if (mode == AppOpsManager.MODE_DEFAULT)
            context.checkCallingOrSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else mode == AppOpsManager.MODE_ALLOWED
    }

    fun checkMainPermission(mainPermission: MainPermission) =
        when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && mainPermission == MainPermission.MANAGE_ALL_FILES ->
                checkManageAllFilesPermission()
            (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) && mainPermission == MainPermission.MANAGE_ALL_FILES ->
                isPermissionGranted(MainPermission.STORAGE.permissionName)
            else -> {
                isPermissionGranted(mainPermission.permissionName)
            }
        }

    private fun isPermissionGranted(permissionName: String) =
        ContextCompat.checkSelfPermission(
            context,
            permissionName
        ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("InlinedApi")
enum class MainPermission(val permissionName: String) {
    CONTACTS(Manifest.permission.READ_CONTACTS),
    STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE),
    MANAGE_ALL_FILES(Manifest.permission.MANAGE_EXTERNAL_STORAGE),

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS)
}