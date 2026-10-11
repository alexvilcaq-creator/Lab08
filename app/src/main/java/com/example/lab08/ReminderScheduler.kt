package com.example.lab08
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun schedule(taskId: Int, description: String, delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf("taskId" to taskId, "description" to description))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("reminder_$taskId", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(taskId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
    }
}