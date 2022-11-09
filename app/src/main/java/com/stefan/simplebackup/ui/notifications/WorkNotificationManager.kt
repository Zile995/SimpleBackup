package com.stefan.simplebackup.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import com.stefan.simplebackup.data.workers.WORK_REQUEST_TAG
import com.stefan.simplebackup.utils.file.BitmapUtil.toBitmap
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

    private val workResultsCounter = WorkResultsCounter()
    private val workManager = WorkManager.getInstance(context.applicationContext)

    private val appsText: (Int) -> String = { numberOfApps ->
        workResultsCounter.run {
            if (numberOfApps > 1) context.getString(R.string.apps) else context.getString(R.string.app)
        }.lowercase()
    }

    init {
        createNotificationChannel()
    }

    override val notificationBuilder by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setOngoing(ongoing)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setContentTitle(context.getString(R.string.work))
            setContentText(context.getString(R.string.work_in_progress))
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(onClickAction())
            addAction(R.drawable.ic_arrow_back, context.getString(R.string.skip), onSkipAction())
            addAction(R.drawable.ic_arrow_back, context.getString(R.string.cancel), onCancelAction())
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
        isBackupNotification: Boolean
    ): Notification = notificationBuilder.run {

        val notificationText =
            workResultsCounter.run {
                val failedText = getFailedText()

                val workTypeText = if (isBackupNotification)
                    context.getString(R.string.backed_up).lowercase()
                else
                    context.getString(R.string.restored).lowercase()

                context.getString(
                    R.string.finished_work_notification,
                    numOfSuccessful,
                    appsText(numOfSuccessful),
                    context.getString(R.string.successfully),
                    failedText,
                    workTypeText
                )
            }

        if (isBackupNotification)
            setContentTitle(context.getString(R.string.backup_completed))
        else
            setContentTitle(context.getString(R.string.restore_completed))

        setOngoing(false)
        setLargeIcon(null)


        val workInfo = workManager.getWorkInfosByTagLiveData(WORK_REQUEST_TAG).value?.first()
        val pendingIntent =
            if (workInfo?.state != WorkInfo.State.RUNNING || workInfo.state != WorkInfo.State.ENQUEUED) {
                onClickAction()
            } else null
        setContentIntent(pendingIntent)
        setAutoCancel(true)
        setContentText(null)
        setExpendableText(notificationText)
        priority = NotificationCompat.PRIORITY_MAX
        setProgress(0, 0, false)
        build()
    }

    private fun getFailedText() =
        workResultsCounter.run {
            if (numOfUnsuccessful > 0) ", $numOfUnsuccessful ${appsText(numOfUnsuccessful)} " +
                    context.getString(R.string.unsuccessfully)
            else ""
        }

    override suspend fun getUpdatedNotification(
        progressData: ProgressData, isBackupNotification: Boolean
    ): Notification = notificationBuilder.apply {
        progressData.apply {
            if (isBackupNotification) setContentTitle("${context.getString(R.string.backing_up)} $name")
            else setContentTitle("${context.getString(R.string.restoring)} $name")
            setLargeIcon(image.toBitmap())
            setExpendableText(message)
            setProgress(PROGRESS_MAX, progressData.progress, false)
            workResult?.let {
                if (workResult == WorkResult.SUCCESS)
                    workResultsCounter.numOfSuccessful++
                else
                    workResultsCounter.numOfUnsuccessful++
            }
        }
    }.build()

    private data class WorkResultsCounter(
        var numOfSuccessful: Int = 0,
        var numOfUnsuccessful: Int = 0
    )
}