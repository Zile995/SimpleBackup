package com.stefan.simplebackup.data.receivers

import android.Manifest
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION_ID
import com.stefan.simplebackup.utils.extensions.parcelable

const val ACTION_WORK_FINISHED = "com.stefan.simplebackup.WORK_FINISHED"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WORK_FINISHED -> {
                NotificationManagerCompat.from(context).cancelAll()
                val notification = intent.extras?.parcelable<Notification>(EXTRA_NOTIFICATION)
                notification?.apply {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        NotificationManagerCompat.from(context)
                            .notify(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0) + 1, this)
                    }
                }
            }
        }
    }
}