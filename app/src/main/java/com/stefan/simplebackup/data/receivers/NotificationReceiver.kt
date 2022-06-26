package com.stefan.simplebackup.data.receivers

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION
import com.stefan.simplebackup.ui.notifications.EXTRA_NOTIFICATION_ID
import kotlinx.coroutines.*

const val ACTION_WORK_FINISHED = "com.stefan.simplebackup.WORK_FINISHED"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WORK_FINISHED -> {
                val notification = intent.getParcelableExtra<Notification>(EXTRA_NOTIFICATION)
                notification?.let {
                    CoroutineScope(Job() + Dispatchers.Main.immediate).launch {
                        NotificationManagerCompat
                            .from(context)
                            .notify(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0) + 1, it)
                        delay(1_000)
                        val workManager = WorkManager.getInstance(context)
                        workManager.pruneWork()
                    }
                }
            }
        }
    }
}