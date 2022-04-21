package com.stefan.simplebackup.data.broadcasts

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION
import com.stefan.simplebackup.ui.notifications.NOTIFICATION_ID

const val ACTION_WORK_FINISHED = "com.stefan.simplebackup.WORK_FINISHED"

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WORK_FINISHED -> {
                val notification = intent.getParcelableExtra<Notification>(EXTRA_NOTIFICATION)
                notification?.let {
                    NotificationManagerCompat
                        .from(context)
                        .notify(NOTIFICATION_ID + 1, it)
                }
            }
        }
    }
}