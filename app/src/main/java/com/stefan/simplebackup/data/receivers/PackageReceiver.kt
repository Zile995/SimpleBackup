package com.stefan.simplebackup.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PackageReceiver(
    private val packageListener: PackageListener,
    private val scope: CoroutineScope
) : BroadcastReceiver() {

    /**
     * - This Broadcast intents works while application is running
     */
    override fun onReceive(context: Context, intent: Intent) {
        scope.launch(ioDispatcher) {
            intent.data?.let {
                val packageName = it.encodedSchemeSpecificPart
                packageListener.apply {
                    when (intent.action) {
                        Intent.ACTION_PACKAGE_ADDED -> {
                            insertOrUpdatePackage(packageName)
                        }
                        Intent.ACTION_PACKAGE_REMOVED -> {
                            deletePackage(packageName)
                        }
                        Intent.ACTION_PACKAGE_REPLACED -> {
                            insertOrUpdatePackage(packageName)
                        }
                    }
                }
            }
        }
    }
}