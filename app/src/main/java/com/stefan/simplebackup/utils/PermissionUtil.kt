package com.stefan.simplebackup.utils

import android.app.Activity
import android.content.Context

object PermissionUtils {
    fun neverAskAgainSelected(activity: Activity, permission: String?): Boolean {
        val prevStatus = getShouldShowRationale(activity, permission)
        val currStatus = activity.shouldShowRequestPermissionRationale(
            permission!!
        )
        return prevStatus != currStatus
    }

    fun setShowDialog(context: Context, permission: String?) {
        val genPrefs = context.getSharedPreferences("storage_permission", Context.MODE_PRIVATE)
        val editor = genPrefs.edit()
        editor.putBoolean(permission, true)
        editor.apply()
    }

    private fun getShouldShowRationale(context: Context, permission: String?): Boolean {
        val genPrefs = context.getSharedPreferences("storage_permission", Context.MODE_PRIVATE)
        // Pokupi preference ako postoji, ako ne postoji postavi ga na false
        return genPrefs.getBoolean(permission, false)
    }
}