package com.stefan.simplebackup.data.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.NOTIFICATION_CANCEL_ACTION
import com.stefan.simplebackup.data.workers.NOTIFICATION_SKIP_ACTION
import com.stefan.simplebackup.data.workers.WORK_NOTIFICATION_ID

class WorkActionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        intent?.apply {
            Log.d("WorkActionBroadcastReceiver", "Got action $action")
            when (action) {
                NOTIFICATION_SKIP_ACTION -> {
                    MainWorker.skipAction?.invoke()
                }
                NOTIFICATION_CANCEL_ACTION -> {
                    // Cancel the notification
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(WORK_NOTIFICATION_ID)

                    // Stop main work and cancel workers
                    MainWorker.cancelAction?.invoke()
                }
            }
        }
    }
}