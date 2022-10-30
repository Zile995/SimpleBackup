package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.utils.work.WorkResult

interface WorkNotificationHelper : NotificationHelper {
    fun getFinishedNotification(isBackupNotification: Boolean = true): Notification

    suspend fun getUpdatedNotification(progressData: ProgressData, isBackupNotification: Boolean)
            : Notification
}