package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.stefan.simplebackup.data.model.AppData

interface NotificationHelper {
    val notificationBuilder: NotificationCompat.Builder

    val notificationId: Int

    fun getFinishedNotification(numberOfPackages: Int = 0, isBackup: Boolean = true): Notification

    fun Context.sendNotificationBroadcast(notification: Notification)

    suspend fun NotificationCompat.Builder.updateNotificationContent(app: AppData): NotificationCompat.Builder
}