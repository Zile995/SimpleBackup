package com.stefan.simplebackup.data.receivers

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION_ID
import com.stefan.simplebackup.utils.extensions.parcelable
import kotlinx.coroutines.*

const val ACTION_WORK_FINISHED = "com.stefan.simplebackup.WORK_FINISHED"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WORK_FINISHED -> {
                val notification = intent.extras?.parcelable<Notification>(EXTRA_NOTIFICATION)
                notification?.let {
                    NotificationManagerCompat
                        .from(context)
                        .notify(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0) + 1, it)
                    MainScope().launch {
                        delay(1_000)
                        val workManager = WorkManager.getInstance(context)
                        workManager.pruneWork()
                    }
                }
            }
        }
    }
}