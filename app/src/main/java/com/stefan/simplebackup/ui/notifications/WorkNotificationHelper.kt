package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.PendingIntent
import com.stefan.simplebackup.data.model.ProgressData

interface WorkNotificationHelper : NotificationHelper {
    fun getFinishedNotification(
        isBackupNotification: Boolean = true
    ): Notification

    suspend fun getUpdatedNotification(progressData: ProgressData, isBackupNotification: Boolean)
            : Notification
}