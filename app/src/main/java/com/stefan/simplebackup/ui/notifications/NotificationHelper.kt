package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stefan.simplebackup.data.receivers.NotificationReceiver

const val EXTRA_NOTIFICATION = "NOTIFICATION_PARCEL"
const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"

interface NotificationHelper {
    val notificationId: Int
    val notificationBuilder: NotificationCompat.Builder

    fun Context.sendNotificationBroadcast(
        notification: Notification,
        actionName: String
    ) {
        applicationContext.sendBroadcast(
            Intent(
                applicationContext,
                NotificationReceiver::class.java
            ).apply {
                action = actionName
                putExtra(
                    EXTRA_NOTIFICATION,
                    notification
                )
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                setPackage(applicationContext.packageName)
            })
    }

    fun NotificationCompat.Builder.setExpendableText(text: String) = run {
        setStyle(NotificationCompat.BigTextStyle().bigText(text))
    }

}