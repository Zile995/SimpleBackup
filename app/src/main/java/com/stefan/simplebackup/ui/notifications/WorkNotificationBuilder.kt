package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.file.BitmapUtil.toBitmap

private const val CHANNEL_ID = "WORK_NOTIFICATION"

class WorkNotificationBuilder(
    private val context: Context,
    private val ongoing: Boolean = true
) : WorkNotificationHelper {

    init {
        createNotificationChannel()
    }

    override val notificationId: Int = 42

    override val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle(context.getString(R.string.work))
            setContentText(context.getString(R.string.work_in_progress))
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

    override fun getFinishedNotification(
        numberOfPackages: Int,
        isBackup: Boolean
    ): Notification {
        return notificationBuilder.apply {
            val appText =
                if (numberOfPackages > 1) context.getString(R.string.apps) else context.getString(R.string.app)
            if (isBackup) {
                setContentTitle(context.getString(R.string.backup_completed))
                setContentText("$numberOfPackages $appText ${context.getString(R.string.successfully_backed_up)}")
            } else {
                setContentTitle(context.getString(R.string.restore_completed))
                setContentText("$numberOfPackages $appText ${context.getString(R.string.successfully_restored)}")
            }
            setOnlyAlertOnce(true)
            setAutoCancel(false)
            setLargeIcon(null)
            setOngoing(false)
            setProgress(0, 0, false)
            priority = NotificationCompat.PRIORITY_MAX
        }.build()
    }

    override suspend fun getUpdatedNotification(notificationData: NotificationData): Notification {
        return notificationBuilder.apply {
            notificationData.apply {
                setContentTitle("${context.getString(R.string.backing_up)} $name")
                setLargeIcon(image.toBitmap())
                setContentText(text)
                setProgress(PROGRESS_MAX, notificationData.progress, false)
            }
        }.build()
    }
}