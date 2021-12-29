package com.stefan.simplebackup.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PackageBroadcastReceiver(
    private val broadcastListener: BroadcastListener,
    private val scope: CoroutineScope
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        scope.launch {
            intent.data?.let {
                val packageName = it.encodedSchemeSpecificPart
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        broadcastListener.addOrUpdatePackages(packageName)
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        broadcastListener.removePackages(packageName)
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        broadcastListener.addOrUpdatePackages(packageName)
                    }
                }
            }
        }
    }
}