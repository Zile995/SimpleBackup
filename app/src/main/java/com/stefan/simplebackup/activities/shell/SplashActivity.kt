package com.stefan.simplebackup.activities.shell

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.stefan.simplebackup.BuildConfig
import com.stefan.simplebackup.activities.MainActivity
import com.stefan.simplebackup.data.AppInfo
import com.stefan.simplebackup.data.Application
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

class SplashActivity : Activity() {
    companion object {
        lateinit var result: Deferred<MutableList<Application>>

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
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                AppInfo.loadPackageManager(this@SplashActivity)
                if (!AppInfo.databaseExists(this@SplashActivity)) {
                    result = async {
                        AppInfo.getInstalledApplications(PackageManager.GET_META_DATA)
                            .setPackageList(this@SplashActivity)
                    }
                }
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