package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import com.stefan.simplebackup.data.model.ProgressData

interface WorkNotificationHelper : NotificationHelper {
    fun getFinishedNotification(
        isBackupNotification: Boolean = true,
        numOfWorkItems: Int
    ): Notification

    suspend fun getUpdatedNotification(progressData: ProgressData, isBackupNotification: Boolean)
            : Notification
}