package com.stefan.simplebackup.utils.extensions

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.FragmentViewBindingDelegate
import com.stefan.simplebackup.ui.fragments.viewpager.BaseViewPagerFragment
import java.lang.reflect.ParameterizedType

// ##
// ## ViewBinding extensions
inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater(layoutInflater)
}

fun <T : ViewBinding> BaseFragment<T>.viewBinding(
) = reflectionViewBinding<T>(::onCleanUp)

fun <T : ViewBinding> BaseViewPagerFragment<T>.viewBinding(
) = reflectionViewBinding<T>(::onCleanUp)

@Suppress("UNCHECKED_CAST")
inline fun <T : ViewBinding> Fragment.reflectionViewBinding(
    crossinline onCleanUp: () -> Unit = {}
): FragmentViewBindingDelegate<T> {
    val vbClass =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>
    val method = vbClass.declaredMethods.first {
        it.parameterTypes.contains(LayoutInflater::class.java)
    }
    val bindingInflater: (LayoutInflater) -> T = { layoutInflater ->
        method.invoke(null, layoutInflater) as T
    }
    return viewBinding(bindingInflater) {
        onCleanUp()
    }
}

inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T, noinline onCleanUp: () -> Unit = {}
): FragmentViewBindingDelegate<T> = FragmentViewBindingDelegate(
    fragment = this, viewBindingFactory = {
        bindingInflater(layoutInflater)
    }, onCleanUp = onCleanUp
)

inline fun <T : ViewBinding> ViewGroup.viewBinding(
    factory: (LayoutInflater, ViewGroup, Boolean) -> T
) = factory(LayoutInflater.from(context), this, false)

// ##
// ## Fragment extensions
fun FragmentManager.getVisibleFragment() =
    findFragmentById(R.id.nav_host_container)?.run {
        childFragmentManager.fragments.firstOrNull { childFragment -> childFragment.isVisible }
    }

inline fun <reified T : Fragment> FragmentManager.findFragmentByClass(): T? =
    fragments.firstOrNull { fragment ->
        fragment is T
    } as T?

inline fun <R> Fragment.onMainActivity(crossinline block: MainActivity.() -> R): R? =
    onActivityCallback<MainActivity, R> {
        block()
    }

@Suppress("UNCHECKED_CAST")
inline fun <T : AppCompatActivity, R> Fragment.onActivityCallback(
    crossinline block: T.() -> R
): R? = (activity as? T)?.run {
    block()
}

//##
//## BroadcastReceiver extensions
fun Context.unregisterReceivers(vararg receivers: BroadcastReceiver) =
    receivers.forEach { receiver ->
        unregisterReceiver(receiver)
    }

// ##
// ## Settings / Common Android Intents
@RequiresApi(Build.VERSION_CODES.R)
fun Context.openManageFilesPermissionSettings() =
    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).run {
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.parse("package:$packageName")
        startActivity(this)
    }

fun Context.openAppNotificationSettings() =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).run {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addCategory(Intent.CATEGORY_DEFAULT)
        startActivity(this)
    }

fun Context.openStorageSettings() =
    Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).run {
        addCategory(Intent.CATEGORY_DEFAULT)
        startActivity(this)
    }

fun Context.openUsageAccessSettings() =
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).run {
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.fromParts("package", packageName, null)
        startActivity(this)
    }

fun Context.openPackageSettingsInfo(packageName: String) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).run {
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.parse("package:$packageName")
        startActivity(this)
    }

fun Context.forceStopPackage(packageName: String) {
    val activityManager =
        applicationContext.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
    activityManager.killBackgroundProcesses(packageName)
    showToast(R.string.application_stopped)
}

fun Context.launchPackage(packageName: String) =
    getLaunchIntent(packageName)?.run {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }

fun Context.uninstallPackage(packageName: String) =
    Intent(Intent.ACTION_DELETE).run {
        data = Uri.parse("package:${packageName}")
        startActivity(this)
    }

fun Context.openFilePath(path: String) =
    Intent(Intent.ACTION_VIEW).run {
        val myDir: Uri = Uri.parse(path)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        setDataAndType(myDir, "resource/folder")
        if (resolveActivity(packageManager) != null)
            startActivity(this)
        else
            showToast(R.string.no_file_managers)
    }

// Package Intents extensions
fun Intent.isPackageAdded() = action == Intent.ACTION_PACKAGE_ADDED

fun Intent.isPackageRemoved() =
    action == Intent.ACTION_PACKAGE_REMOVED && extras?.getBoolean(Intent.EXTRA_REPLACING) == false

// ##
// ## Resource and toast extensions
fun Context.showToast(@StringRes resId: Int, longDuration: Boolean = false) =
    showToast(getString(resId), longDuration)

fun Context.showToast(message: String, longDuration: Boolean = false) {
    val duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(applicationContext, message, duration).show()
}

fun Context.getResourceDrawable(@DrawableRes drawable: Int) =
    ContextCompat.getDrawable(this, drawable)

fun AppCompatActivity.setStatusBarColor(@ColorRes color: Int) {
    window.statusBarColor = getColorFromResource(color)
}

fun Context.getColorFromResource(@ColorRes color: Int) =
    ContextCompat.getColor(applicationContext, color)

fun Context.getInterFontTypeFace() =
    ResourcesCompat.getFont(applicationContext, R.font.inter_family)

// ##
// ## Intent / Bundle extensions
inline fun <reified T : AppCompatActivity> Context.launchActivity(
    vararg values: Pair<String, Any?> = emptyArray(),
    customFlags: Int = Intent.FLAG_ACTIVITY_SINGLE_TOP
) {
    Intent(applicationContext, T::class.java).apply {
        putExtras(bundleOf(*values))
        flags = customFlags
        startActivity(this)
    }
}

fun Context.getLaunchIntent(packageName: String = getPackageName()) =
    packageManager.getLaunchIntentForPackage(packageName)

fun Context.getLastActivityIntent(): PendingIntent {
    val intent = getLaunchIntent()
    return PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

inline fun intentFilter(vararg actions: String, crossinline block: IntentFilter.() -> Unit = {}) =
    IntentFilter().apply {
        actions.forEach { action ->
            addAction(action)
        }
        block()
    }

inline fun <reified T : Enum<T>> Bundle.putEnumInt(victim: T) =
    apply { putInt(T::class.java.name, victim.ordinal) }

inline fun <reified T : Enum<T>> Bundle.getEnumInt() =
    getInt(T::class.java.name, -1).takeUnless { ordinal -> ordinal == -1 }
        ?.let { T::class.java.enumConstants?.get(it) }

inline fun <reified T : Enum<T>> Fragment.putEnumExtra(victim: T) {
    arguments = Bundle().putEnumInt(victim)
}

inline fun <reified T : Enum<T>> Fragment.getEnumExtra(): T? = arguments?.getEnumInt<T>()

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <T> AppCompatActivity.fromIntentExtras(block: Bundle.() -> T) = intent.extras?.run {
    block()
}

// ##
// ## Dialog builder
inline fun Context.materialDialog(
    title: String,
    message: String,
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    enablePositiveButton: Boolean = true,
    enableNegativeButton: Boolean = true,
    crossinline onPositiveButtonPress: () -> Unit = {},
    crossinline onNegativeButtonPress: () -> Unit = {},
    @ColorInt positiveColor: Int = getColor(R.color.positive_dialog_text),
    @ColorInt negativeColor: Int = getColor(R.color.negative_dialog_text)
) = MaterialAlertDialogBuilder(this, R.style.Widget_SimpleBackup_MaterialAlertDialog).run {
    setTitle(title)
    setMessage(message)
    if (enablePositiveButton) {
        setPositiveButton(positiveButtonText) { _, _ ->
            onPositiveButtonPress()
        }
    }
    if (enableNegativeButton) {
        setNegativeButton(negativeButtonText) { _, _ ->
            onNegativeButtonPress()
        }
    }
    val alert = create()
    alert.apply {
        setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(positiveColor)
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(negativeColor)
        }
    }
}