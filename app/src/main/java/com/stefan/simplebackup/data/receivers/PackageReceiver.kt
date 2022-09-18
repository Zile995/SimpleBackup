package com.stefan.simplebackup.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PackageReceiver(
    private val scope: CoroutineScope,
    private val packageListener: PackageListener
) : BroadcastReceiver() {

    /**
     * - This Broadcast intents works while application is running
     */
    override fun onReceive(context: Context, intent: Intent) {
        scope.launch(ioDispatcher) {
            intent.data?.apply {
                val packageName = encodedSchemeSpecificPart
                packageListener.apply {
                    when {
                        intent.action == Intent.ACTION_PACKAGE_ADDED -> {
                            onActionPackageAdded(packageName)
                        }
                        intent.action == Intent.ACTION_PACKAGE_REMOVED &&
                                intent.extras?.getBoolean(Intent.EXTRA_REPLACING) == false -> {
                            onActionPackageRemoved(packageName)
                        }
                    }
                }
            }
        }
    }
}