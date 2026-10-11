package com.example.lab08
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val description = inputData.getString("description") ?: return Result.failure()
        val taskId = inputData.getInt("taskId", 0)
        val channelId = "task_reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Recordatorios de tareas", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tarea pendiente")
            .setContentText(description)
            .setAutoCancel(true)
            .build()

        if (manager.areNotificationsEnabled()) {
            manager.notify(taskId, notification)
        }
        return Result.success()
    }
}