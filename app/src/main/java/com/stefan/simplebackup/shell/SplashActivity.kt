package com.stefan.simplebackup.shell

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.stefan.simplebackup.BuildConfig
import com.stefan.simplebackup.MainActivity
import com.stefan.simplebackup.data.AppInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : Activity() {
    companion object {
        init {
            // Set settings before the main shell can be created
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            val builder: Shell.Builder = Shell.Builder.create()
            Shell.setDefaultBuilder(
                builder
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                AppInfo.loadPackageManager(this@SplashActivity)
                    .getInstalledApplications(PackageManager.GET_META_DATA)
            }
            // Preheat the main root shell in the splash screen
            // so the app can use it afterwards without interrupting
            // application flow (e.g. root permission prompt)
            launch {
                Shell.getShell {
                    // The main shell is now constructed and cached
                    // Exit splash screen and enter main activity
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}