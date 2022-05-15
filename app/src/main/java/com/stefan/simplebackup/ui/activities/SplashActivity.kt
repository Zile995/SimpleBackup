package com.stefan.simplebackup.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.stefan.simplebackup.BuildConfig
import com.topjohnwu.superuser.Shell

class SplashActivity : Activity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Preheat the main root shell in the splash screen
        // so the app can use it afterwards without interrupting
        // application flow (e.g. root permission prompt)
        Shell.getShell {
            // The main shell is now constructed and cached
            // Exit splash screen and enter main activity
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}