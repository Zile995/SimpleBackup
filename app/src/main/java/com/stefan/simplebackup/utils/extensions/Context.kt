package com.stefan.simplebackup.utils.extensions

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.stefan.simplebackup.R
import kotlinx.coroutines.launch

fun <T : AppCompatActivity> FragmentActivity.onActivityCallbacks(block: suspend T.() -> Unit) {
    @Suppress("UNCHECKED_CAST")
    (this as T).apply {
        lifecycleScope.launch {
            block()
        }
    }
}

inline fun <reified T : AppCompatActivity> Context.passBundleToActivity(
    value: Pair<String, Any?>
) {
    Intent(this, T::class.java).apply {
        putExtras(bundleOf(value))
        startActivity(this)
    }
}

fun Context.deletePackage(packageName: String) {
    startActivity(Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:${packageName}")
    })
}

fun Context.forceStopPackage(packageName: String) {
    val activityManager =
        applicationContext.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
    activityManager.killBackgroundProcesses(packageName)
    showToast(R.string.application_stopped)
}

fun Context.openPackageSettingsInfo(packageName: String) {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.parse("package:$packageName")
    })
}

fun Context.launchPackage(packageName: String) {
    val intent = applicationContext.packageManager.getLaunchIntentForPackage(packageName)
    intent?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

fun Context.showToast(@StringRes message: Int, longDuration: Boolean = false) {
    val duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(applicationContext, message, duration).show()
}

fun Context.showToast(message: String, longDuration: Boolean = false) {
    val duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(applicationContext, message, duration).show()
}

fun Context.getResourceDrawable(@DrawableRes drawable: Int) =
    ContextCompat.getDrawable(this, drawable)

fun Context.workerDialog(
    title: String,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String,
    doWork: () -> Unit
) {
    val builder = AlertDialog.Builder(this, R.style.DialogTheme)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(positiveButtonText) { dialog, _ ->
        dialog.cancel()
        doWork()
    }
    builder.setNegativeButton(negativeButtonText) { dialog, _ -> dialog.cancel() }
    val alert = builder.create()
    alert.setOnShowListener {
        alert.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(this.getColor(R.color.negativeDialog))
        alert.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(this.getColor(R.color.positiveDialog))
    }
    alert.show()
}


fun Context.permissionDialog(
    title: String,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String,
) {
    val builder = AlertDialog.Builder(this, R.style.DialogTheme)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(positiveButtonText) { dialog, _ ->
        dialog.cancel()
    }
    builder.setNegativeButton(negativeButtonText) { dialog, _ ->
        dialog.cancel()
        val uriData = Uri.parse("package:${applicationContext.packageName}")
        if (SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = uriData
            })
        } else {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = uriData
            })
        }
    }
    val alert = builder.create()
    alert.setOnShowListener {
        alert.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(this.getColor(R.color.negativeDialog))
        alert.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(this.getColor(R.color.positiveDialog))
    }
    alert.show()
}