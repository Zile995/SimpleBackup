package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.AppData

const val NOTIFICATION_ID = 42
const val CHANNEL_ID = "BACKUP_NOTIFICATION"

class BackupNotificationBuilder(private val context: Context, private val ongoing: Boolean) {

    init {
        createNotificationChannel()
    }

    private val progressNotificationBuilder: NotificationCompat.Builder by lazy {
        getNotificationBuilder()
    }
    val getProgressNotificationBuilder get() = progressNotificationBuilder

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

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("Backup")
            setContentText("Backup in progress")
            setSmallIcon(R.drawable.backup_notification_24dp)
            setOnlyAlertOnce(true)
            setOngoing(ongoing)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }

    fun createNotification(): Notification = progressNotificationBuilder.build()

    fun showBackupFinishedNotification() {
        getNotificationManager.notify(
            getNotificationId,
            progressNotificationBuilder.apply {
                setContentTitle("Backup Completed")
                setContentText("Apps successfully backed up")
                setOnlyAlertOnce(true)
                setAutoCancel(false)
                setLargeIcon(null)
                setOngoing(false)
                setProgress(0, 0, false)
                priority = NotificationCompat.PRIORITY_MAX
            }.build()
        )
    }

    fun showNotification() {
        getNotificationManager
            .notify(getNotificationId, createNotification())
    }

    fun NotificationCompat.Builder.updateNotificationContent(app: AppData): NotificationCompat.Builder {
        progressNotificationBuilder.apply {
            setContentTitle("Backing up ${app.name}")
            setLargeIcon(
                BitmapFactory.decodeByteArray(
                    app.bitmap,
                    0,
                    app.bitmap.size
                )
            )
            return this
        }
    }
}