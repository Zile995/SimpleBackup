package com.stefan.simplebackup.data.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PackageBroadcastReceiver(
    private val packageListener: PackageListener,
    private val scope: CoroutineScope
) : BroadcastReceiver() {

    /**
     * - This Broadcast intents works while application is running
     */
    override fun onReceive(context: Context, intent: Intent) {
        scope.launch {
            intent.data?.let {
                val packageName = it.encodedSchemeSpecificPart
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        packageListener.addOrUpdatePackage(packageName)
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        packageListener.deletePackage(packageName)
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        packageListener.addOrUpdatePackage(packageName)
                    }
                }
            }
        }
    }
}