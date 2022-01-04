package com.stefan.simplebackup.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.stefan.simplebackup.workers.BootPackageWorker

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageSharedPref = context.getSharedPreferences("package", Context.MODE_PRIVATE)
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            packageSharedPref.apply {
                edit()
                    .putInt("sequence_number", 0)
                    .apply()
            }
            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(OneTimeWorkRequest.from(BootPackageWorker::class.java))
        }
    }
}