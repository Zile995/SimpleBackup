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
) : WorkResultAction {

    /**
     * - Index of the processed item
     */
    private var workItemIndex = 0

    /**
     * - Current progress value which will be updated
     */
    private var currentProgress = 0

    /**
     * - List of progress intervals
     * - The list will be filled with the max progress value of each item
     */
    private val progressIntervals = mutableListOf<Int>()

    /**
     * - Interval, or max progress value of one item
     * - It is used, internally, for updating the [currentProgress]
     */
    private val itemInterval = PROGRESS_MAX / workItems.size

    /**
     * - Lambda function which updates the [currentProgress]
     */
    private val updateProgress = { steps: Int ->
        currentProgress += itemInterval / steps
    }

    init {
        // Generate all work items intervals
        generateIntervals()
    }

    override suspend fun onFailure(app: AppData) {
        setNearestItemInterval()
    }

    /**
     * Method which saves intervals for each work item
     */
    private fun generateIntervals() {
        var intervalSum = 0
        repeat(workItems.size) {
            intervalSum += itemInterval
            progressIntervals.add(intervalSum)
        }
    }

    /**
     * - Method which sets [currentProgress] to nearest progress interval
     * - This method is used when the work item does not exist or when the work operation fails
     */
    private fun setNearestItemInterval() {
        currentProgress = progressIntervals.first { interval ->
            interval >= currentProgress
        }
    }

    /**
     * - Main work method which executes work actions and handles work exceptions
     */
    protected suspend fun AppData?.startWork(
        vararg actions: suspend (AppData) -> Unit
    ) {
        workItemIndex++
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
                    onSuccess(app = this)
                } catch (e: IOException) {
                    Log.w("WorkUtil", "Oh, an error occurred: $e")
                    onFailure(app = this)
                }
            }
        }
    }

    protected suspend fun AppData.updateProgressData(
        @StringRes progressText: Int,
        workResult: WorkResult? = null
    ) {
        val progressMessage = appContext.getString(progressText)
        val progressData = ProgressData(
            name = name,
            image = bitmap,
            index = workItemIndex,
            workResult = workResult,
            message = progressMessage,
            packageName = packageName,
            progress = currentProgress
        )
        updateForegroundInfo(progressData)
    }
}

private interface WorkResultAction {
    suspend fun onSuccess(app: AppData)
    suspend fun onFailure(app: AppData)
}

enum class WorkResult {
    SUCCESS,
    ERROR
}