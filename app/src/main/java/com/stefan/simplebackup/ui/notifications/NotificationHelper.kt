package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.stefan.simplebackup.data.model.NotificationData

interface NotificationHelper {
    val notificationId: Int
    val notificationBuilder: NotificationCompat.Builder

    fun getFinishedNotification(numberOfPackages: Int = 0, isBackup: Boolean = true): Notification

    fun Context.sendNotificationBroadcast(notification: Notification)

    suspend fun NotificationCompat.Builder.getUpdatedNotification(notificationData: NotificationData)
            : Notification
}