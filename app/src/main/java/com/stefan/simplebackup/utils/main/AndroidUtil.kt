package com.stefan.simplebackup.utils.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.backup.ROOT
import com.stefan.simplebackup.utils.file.BitmapUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow

const val PARCELABLE_EXTRA = "application"
val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

fun Number.transformBytesToString(): String {
    return String.format("%3.1f %s", this.toFloat() / 1_000.0.pow(2), "MB")
}

inline fun <reified T : AppCompatActivity> Context?.passParcelableToActivity(
    app: AppData,
    scope: CoroutineScope
) {
    this?.let { context ->
        scope.launch {
            val intent = Intent(context, T::class.java)
                .putExtra(PARCELABLE_EXTRA, BitmapUtil.appWithCheckedBitmap(app, context))
            context.startActivity(intent)
        }
    }
}

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

fun Context.deletePackage(packageName: String) {
    startActivity(Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:${packageName}")
    })
}

fun Context.forceStopPackage(packageName: String) {
    val activityManager =
        applicationContext.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
    activityManager.killBackgroundProcesses(packageName)
    showToast("Application stopped!")
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

fun Context.showToast(message: String) {
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
}

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
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("package:${applicationContext.packageName}")
        })
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

fun ImageView.loadBitmap(byteArray: ByteArray) {
    val image = this
    GlideApp.with(image).apply {
        asBitmap()
            .load(byteArray)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(image)
    }
}

fun RecyclerView.hideAttachedButton(floatingButton: FloatingActionButton) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy > 0 && floatingButton.isShown) {
                floatingButton.hide()

            } else if (dy < 0 && !floatingButton.isShown) {
                floatingButton.show()
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            // Ako ne može da skroluje na dole (1 je down direction) i ako može ka gore (-1 up direction)
            if (!recyclerView.canScrollVertically(-1)) {
                floatingButton.hide()
            }
        }
    })
}