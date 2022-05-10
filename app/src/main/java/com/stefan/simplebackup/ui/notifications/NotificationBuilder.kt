package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.broadcasts.ACTION_WORK_FINISHED
import com.stefan.simplebackup.data.broadcasts.NotificationBroadcastReceiver
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.BitmapUtil
import kotlinx.coroutines.withContext

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
        setNotificationBuilder()
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

    private fun setNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle("Work")
            setContentText("Work in progress")
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            setOngoing(ongoing)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }

    override fun Context.sendNotificationBroadcast(
        notification: Notification
    ) {
        applicationContext.sendBroadcast(
            Intent(
                applicationContext,
                NotificationBroadcastReceiver::class.java
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
            if (isBackup) {
                setContentTitle("Backup Completed")
                setContentText("$numberOfPackages apps successfully backed up")
            } else {
                setContentTitle("Restore Completed")
                setContentText("$numberOfPackages apps successfully restored")
            }
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            foregroundServiceBehavior = FOREGROUND_SERVICE_IMMEDIATE
            setLargeIcon(null)
            setOngoing(false)
            setProgress(0, 0, false)
            priority = NotificationCompat.PRIORITY_MAX
        }.build()
    }

    override suspend fun NotificationCompat.Builder.updateNotificationContent(app: AppData): NotificationCompat.Builder {
        return apply {
            setContentTitle("Backing up ${app.name}")
            setLargeIcon(BitmapUtil.byteArrayToBitmap(app))
        }
    }
}