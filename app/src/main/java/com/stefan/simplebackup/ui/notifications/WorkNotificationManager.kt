package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.utils.extensions.toBitmap
import com.stefan.simplebackup.utils.work.WorkResult


private const val CHANNEL_ID = "WORK_NOTIFICATION"

class WorkNotificationManager(
    private val context: Context,
    override val notificationId: Int,
    private val ongoing: Boolean = true,
    private val onClickAction: () -> PendingIntent,
    private val onSkipAction: () -> PendingIntent,
    private val onCancelAction: () -> PendingIntent
) : WorkNotificationHelper {

    private val resultTextProvider = ResultTextProvider(context)

    init {
        createNotificationChannel()
    }

    override val notificationBuilder by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setOngoing(ongoing)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setContentTitle(context.getString(R.string.work))
            setContentIntent(onClickAction())
            setContentText(context.getString(R.string.work_in_progress))
            setSmallIcon(R.drawable.ic_launcher_foreground)
            addAction(R.drawable.ic_skip, context.getString(R.string.skip), onSkipAction())
            addAction(R.drawable.ic_cancel, context.getString(R.string.cancel), onCancelAction())
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
        isBackupNotification: Boolean,
        numOfWorkItems: Int
    ): Notification {
        val notificationText =
            resultTextProvider.getResultText(isBackupNotification, numOfWorkItems)

        val title = getFinishedTitle(isBackupNotification)

        return notificationBuilder.buildFinishedNotification(
            title = title,
            expendableText = notificationText,
            onClickIntent = onClickAction()
        )
    }

    override suspend fun getUpdatedNotification(
        progressData: ProgressData,
        isBackupNotification: Boolean
    ): Notification = progressData.run {
        resultTextProvider.checkWorkResult(workResult)

        val title = if (isBackupNotification)
            "${context.getString(R.string.backing_up)} $name"
        else
            "${context.getString(R.string.restoring)} $name"

        notificationBuilder.buildUpdatedNotification(
            title = title,
            image = image.toBitmap(),
            progress = progress,
            expendableText = message
        )
    }

    private fun getFinishedTitle(isBackupNotification: Boolean) =
        if (isBackupNotification)
            context.getString(R.string.backup_completed)
        else
            context.getString(R.string.restore_completed)

    private fun NotificationCompat.Builder.buildFinishedNotification(
        title: String,
        expendableText: String,
        onClickIntent: PendingIntent?
    ) = run {
        clearActions()
        setOngoing(false)
        setLargeIcon(null)
        setAutoCancel(true)
        setContentText(null)
        setContentTitle(title)
        setContentIntent(onClickIntent)
        setExpendableText(expendableText)
        priority = NotificationCompat.PRIORITY_MAX
        setProgress(0, 0, false)
        build()
    }

    private fun NotificationCompat.Builder.buildUpdatedNotification(
        title: String,
        image: Bitmap,
        progress: Int,
        expendableText: String
    ) = run {
        setLargeIcon(image)
        setContentTitle(title)
        setExpendableText(expendableText)
        setProgress(PROGRESS_MAX, progress, false)
        build()
    }

    private class ResultTextProvider(private val context: Context) {
        private var numOfSuccessful: Int = 0

        fun checkWorkResult(workResult: WorkResult?) = workResult?.let {
            if (it == WorkResult.SUCCESS)
                numOfSuccessful++
        }

        fun getResultText(isBackupNotification: Boolean, numOfWorkItems: Int): String {
            val baseText = generateBaseText(numOfWorkItems).lowercase()
            val finishedWorkTypeText = getFinishedWorkTypeText(isBackupNotification).lowercase()
            return "$baseText $finishedWorkTypeText"
        }

        private fun generateBaseText(numOfWorkItems: Int): String {
            val numOfUnsuccessful = numOfWorkItems - numOfSuccessful
            return when {
                numOfSuccessful > 0 && numOfUnsuccessful > 0 -> {
                    getSuccessfulText(numOfSuccessful) + ", " +
                            getUnsuccessfulText(numOfUnsuccessful)
                }
                numOfSuccessful > 0 && numOfUnsuccessful == 0 -> {
                    getSuccessfulText(numOfSuccessful)
                }
                numOfUnsuccessful > 0 -> {
                    getUnsuccessfulText(numOfUnsuccessful)
                }
                else -> getUnsuccessfulText(numOfWorkItems)
            }
        }

        private fun getSuccessfulText(numOfSuccessful: Int): String =
            "$numOfSuccessful ${getAppsText(numOfSuccessful)} ${context.getString(R.string.successfully)}"

        private fun getUnsuccessfulText(numOfUnsuccessful: Int): String =
            "$numOfUnsuccessful ${getAppsText(numOfUnsuccessful)} ${context.getString(R.string.unsuccessfully)}"

        private fun getAppsText(numOfItems: Int) = when {
            numOfItems > 1 -> context.getString(R.string.apps)
            else -> context.getString(R.string.app)
        }

        private fun getFinishedWorkTypeText(isBackupNotification: Boolean) =
            if (isBackupNotification)
                context.getString(R.string.backed_up)
            else
                context.getString(R.string.restored)
    }
}