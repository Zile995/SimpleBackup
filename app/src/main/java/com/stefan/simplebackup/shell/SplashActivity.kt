package com.stefan.simplebackup.shell

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.stefan.simplebackup.BuildConfig
import com.stefan.simplebackup.MainActivity
import com.topjohnwu.superuser.Shell

class SplashActivity : Activity() {
    companion object {
        init {
            // Set settings before the main shell can be created
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}