package com.stefan.simplebackup.ui.activities

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import com.stefan.simplebackup.BuildConfig
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.openUsageAccessSettings
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        init {
            // Set settings before the main shell can be created
            val builder = Shell.Builder.create()
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                builder
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }

    private var isUsageStatsGranted: Boolean by Delegates.observable(false) { _, _, newGrantedStatus ->
        if (newGrantedStatus) {
            // Preheat the main root shell in the splash screen
            // so the app can use it afterwards without interrupting
            // application flow (e.g. root permission prompt)
            launchOnViewLifecycle {
                launch(ioDispatcher) {
                    Shell.getShell()
                }.join()
                // The main shell is now constructed and cached
                // Exit splash screen and enter main activity
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            openUsageAccessSettings()
        }
    }

    override fun onStart() {
        super.onStart()
        checkUsageStatsPermission()
    }

    private val appOpsService by lazy { getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager }

    private fun checkUsageStatsPermission() {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appOpsService.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        else
            appOpsService.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        isUsageStatsGranted = if (mode == AppOpsManager.MODE_DEFAULT)
            checkCallingOrSelfPermission(PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        else mode == AppOpsManager.MODE_ALLOWED
    }
}