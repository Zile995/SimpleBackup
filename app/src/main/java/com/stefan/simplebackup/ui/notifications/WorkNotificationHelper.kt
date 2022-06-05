package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import com.stefan.simplebackup.data.model.NotificationData

interface WorkNotificationHelper : NotificationHelper {
    fun getFinishedNotification(numberOfPackages: Int = 0, isBackup: Boolean = true): Notification

    suspend fun getUpdatedNotification(notificationData: NotificationData)
            : Notification
}