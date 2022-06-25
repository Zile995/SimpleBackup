package com.stefan.simplebackup.utils.extensions

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.FragmentViewBindingDelegate
import kotlinx.coroutines.launch

val Context.getResourceString: (Int) -> String
    get() = { resource ->
        this.getString(resource)
    }

inline fun intentFilter(vararg actions: String, crossinline block: IntentFilter.() -> Unit = {}) =
    IntentFilter().apply {
        actions.forEach { action ->
            addAction(action)
        }
        block()
    }

fun Context.unregisterReceivers(vararg receivers: BroadcastReceiver) {
    receivers.forEach { receiver ->
        unregisterReceiver(receiver)
    }
}

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater(layoutInflater)
    }

inline fun <T : ViewBinding> BaseFragment<T>.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    viewBinding(bindingInflater) {
        onCleanUp()
    }

inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T,
    noinline cleanUp: () -> Unit = {}
): FragmentViewBindingDelegate<T> = FragmentViewBindingDelegate(
    this,
    viewBindingFactory = {
        bindingInflater(layoutInflater)
    },
    cleanUp = cleanUp
)

inline fun <T : ViewBinding> ViewGroup.viewBinding(
    factory: (LayoutInflater, ViewGroup, Boolean) -> T
) =
    factory(LayoutInflater.from(context), this, false)

inline fun <reified T : AppCompatActivity> Context.passBundleToActivity(
    value: Pair<String, Any?>
) {
    Intent(this, T::class.java).apply {
        putExtras(bundleOf(value))
        startActivity(this)
    }
}

inline fun <T : AppCompatActivity> Fragment.onActivityCallback(
    crossinline block: suspend T.() -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    (activity as? T)?.apply {
        viewLifecycleOwner.lifecycleScope.launch {
            block()
        }
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