package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.utils.work.backup.WorkResult

interface WorkNotificationHelper : NotificationHelper {
    fun getFinishedNotification(
        results: List<WorkResult>,
        isBackupNotification: Boolean = true
    ): Notification

    suspend fun getUpdatedNotification(notificationData: NotificationData)
            : Notification
}