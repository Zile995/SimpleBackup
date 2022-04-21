package com.stefan.simplebackup.data.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.stefan.simplebackup.data.workers.BootPackageWorker

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(OneTimeWorkRequest.from(BootPackageWorker::class.java))
        }
    }
}