package com.stefan.simplebackup.utils.work

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.model.ProgressData
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

    private var currentWorkItemIndex = 0
    protected val updateProgress = { steps: Int ->
        currentProgress += perItemInterval / steps
    }

    init {
        generateIntervals()
    }

    abstract suspend fun AppData.onSuccess()
    abstract suspend fun AppData.onFailure()

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
    ) {
        currentWorkItemIndex++
        when {
            this == null -> {
                setNearestItemInterval()
            }
            else -> {
                try {
                    actions.forEach { action ->
                        action(this)
                        updateProgress(actions.size)
                    }
                    onSuccess()
                } catch (e: IOException) {
                    Log.w("WorkUtil", "Oh, an error occurred: $e")
                    setNearestItemInterval()
                    onFailure()
                }
            }
        }
    }

    protected suspend fun AppData.updateNotificationData(
        @StringRes progressText: Int,
        workResult: WorkResult? = null
    ) {
        val progressMessage = appContext.getString(progressText)
        val progressData = ProgressData(
            index = currentWorkItemIndex,
            name = name,
            image = bitmap,
            message = progressMessage,
            progress = currentProgress,
            workResult = workResult
        )
        updateForegroundInfo(progressData)
    }
}

enum class WorkResult {
    SUCCESS,
    ERROR
}