package com.stefan.simplebackup.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.stefan.simplebackup.data.workers.BootPackageWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(OneTimeWorkRequest.from(BootPackageWorker::class.java))
        }
    }
}