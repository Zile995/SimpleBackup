package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationManagerCompat
import com.stefan.simplebackup.R
import com.stefan.simplebackup.domain.model.AppData

const val NOTIFICATION_ID = 42
const val EXTRA_NOTIFICATION = "NOTIFICATION_PARCEL"
const val CHANNEL_ID = "MAIN_NOTIFICATION"

class NotificationBuilder(private val context: Context, private val ongoing: Boolean) {

    init {
        createNotificationChannel()
    }

    private val builder: NotificationCompat.Builder by lazy {
        setNotificationBuilder()
    }
    val getBuilder get() = builder

    private val getNotificationManager: NotificationManagerCompat
        get() {
            return NotificationManagerCompat.from(context)
        }

    val getNotificationId get() = NOTIFICATION_ID

    private fun createNotificationChannel() {
        val name = context.getString(R.string.notification_channel_name)
        val descriptionText = context.getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("Backup")
            setContentText("Backup in progress")
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            setOngoing(ongoing)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }

    fun showBackupFinishedNotification(size: Int) {
        getNotificationManager.notify(
            NOTIFICATION_ID,
            getBackupFinishedNotification(size)
        )
    }

    fun getBackupFinishedNotification(size: Int): Notification {
        return builder.apply {
            setContentTitle("Backup Completed")
            setContentText("$size apps successfully backed up")
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            foregroundServiceBehavior = FOREGROUND_SERVICE_IMMEDIATE
            setLargeIcon(null)
            setOngoing(false)
            setProgress(0, 0, false)
            priority = NotificationCompat.PRIORITY_MAX
        }.build()
    }

    fun showMainNotification() {
        getNotificationManager
            .notify(getNotificationId, builder.build())
    }

    fun NotificationCompat.Builder.updateNotificationContent(app: AppData): NotificationCompat.Builder {
        return this.apply {
            setContentTitle("Backing up ${app.name}")
            setLargeIcon(
                BitmapFactory.decodeByteArray(
                    app.bitmap,
                    0,
                    app.bitmap.size
                )
            )
        }
    }
}