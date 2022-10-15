package com.stefan.simplebackup.utils.work

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.NotificationData
import com.stefan.simplebackup.data.workers.ForegroundCallback
import com.stefan.simplebackup.data.workers.PROGRESS_MAX
import java.io.IOException

abstract class WorkUtil(
    private val appContext: Context,
    private val workItems: Array<String>,
    private val updateForegroundInfo: ForegroundCallback
) {
    // Progress variables
    private var currentProgress = 0
    private val generatedIntervals = mutableListOf<Int>()
    private val perItemInterval = PROGRESS_MAX / workItems.size
    protected val updateProgress = { steps: Int ->
        currentProgress += perItemInterval / steps
    }

    init {
        generateIntervals()
    }

    abstract fun updateWhenAppDoesNotExists(): WorkResult
    abstract suspend fun AppData.updateOnSuccess(): WorkResult
    abstract suspend fun AppData.updateOnFailure(): WorkResult

    private fun generateIntervals() {
        var intervalSum = 0
        repeat(workItems.size) {
            intervalSum += perItemInterval
            generatedIntervals.add(intervalSum)
        }
    }

    protected fun setNearestItemInterval() {
        currentProgress = generatedIntervals.first { interval ->
            interval > currentProgress
        }
    }

    protected suspend fun AppData?.startWork(
        vararg actions: suspend (AppData) -> Unit
    ): WorkResult {
        return when {
            this == null -> {
                updateWhenAppDoesNotExists()
            }
            else -> {
                try {
                    actions.forEach { action ->
                        action(this)
                        updateProgress(actions.size)
                    }
                    updateOnSuccess()
                } catch (e: IOException) {
                    Log.w("WorkUtil", "Oh, an error occurred: $e")
                    updateOnFailure()
                }
            }
        }
    }

    protected suspend fun AppData.updateNotificationData(@StringRes info: Int) {
        val text = appContext.getString(info)
        val notificationData =
            NotificationData(
                name = name,
                text = text,
                image = bitmap,
                progress = currentProgress
            )
        updateForegroundInfo(notificationData)
    }
}

enum class WorkResult {
    SUCCESS,
    ERROR
}