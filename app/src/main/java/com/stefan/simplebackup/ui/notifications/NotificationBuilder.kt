package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.receivers.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.receivers.NotificationReceiver
import com.stefan.simplebackup.utils.file.BitmapUtil.toBitmap

const val NOTIFICATION_ID = 42
const val EXTRA_NOTIFICATION = "NOTIFICATION_PARCEL"
const val CHANNEL_ID = "MAIN_NOTIFICATION"

class NotificationBuilder(
    private val context: Context,
    private val ongoing: Boolean = true
) : NotificationHelper {

    init {
        createNotificationChannel()
    }

    override val notificationId: Int
        get() = NOTIFICATION_ID

    override val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("Work")
            setContentText("Work in progress")
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            setOngoing(ongoing)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }

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

    override fun Context.sendNotificationBroadcast(
        notification: Notification
    ) {
        applicationContext.sendBroadcast(
            Intent(
                applicationContext,
                NotificationReceiver::class.java
            ).apply {
                action = ACTION_WORK_FINISHED
                putExtra(
                    EXTRA_NOTIFICATION,
                    notification
                )
                setPackage(applicationContext.packageName)
            })
    }

    override fun getFinishedNotification(
        numberOfPackages: Int,
        isBackup: Boolean
    ): Notification {
        return notificationBuilder.apply {
            val appText = if (numberOfPackages > 1) "apps" else "app"
            if (isBackup) {
                setContentTitle("Backup Completed")
                setContentText("$numberOfPackages $appText successfully backed up")
            } else {
                setContentTitle("Restore Completed")
                setContentText("$numberOfPackages $appText successfully restored")
            }
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            setLargeIcon(null)
            setOngoing(false)
            setProgress(0, 0, false)
            priority = NotificationCompat.PRIORITY_MAX
        }.build()
    }

    override suspend fun NotificationCompat.Builder.updateNotificationContent(notificationData: NotificationData): NotificationCompat.Builder {
        return apply {
            notificationData.apply {
                setContentTitle("Backing up $name")
                setLargeIcon(image.toBitmap())
                setContentText(text)
            }
        }
    }
}