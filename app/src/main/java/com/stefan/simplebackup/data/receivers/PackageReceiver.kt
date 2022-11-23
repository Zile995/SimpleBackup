package com.stefan.simplebackup.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stefan.simplebackup.utils.extensions.isPackageAdded
import com.stefan.simplebackup.utils.extensions.isPackageRemoved
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageReceiver(
    private val scope: CoroutineScope,
    private val packageListener: PackageListener
) : BroadcastReceiver() {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * - This Broadcast intents works while application is running
     */
    override fun onReceive(context: Context, intent: Intent) {
        scope.launch(ioDispatcher) {
            intent.data?.apply {
                val packageName = encodedSchemeSpecificPart
                packageListener.apply {
                    when {
                        intent.isPackageAdded() -> {
                            onActionPackageAdded(packageName)
                        }
                        intent.isPackageRemoved() -> {
                            onActionPackageRemoved(packageName)
                        }
                    }
                }
            }
        }
    }
}