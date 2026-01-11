package dev.tuandoan.tasktracker.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tuandoan.tasktracker.MainActivity
import dev.tuandoan.tasktracker.TaskTrackerApplication

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        private const val TAG = "TaskReminder"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "TaskReminderWorker.doWork() started")

        return try {
            val taskId = inputData.getLong(KEY_TASK_ID, -1)
            val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Task"

            Log.d(TAG, "Processing reminder for task $taskId: '$taskTitle'")

            if (taskId == -1L) {
                Log.e(TAG, "Invalid task ID: $taskId")
                return Result.failure()
            }

            // Check notification permissions
            val notificationsEnabled = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            Log.d(TAG, "Notifications enabled: $notificationsEnabled")

            showNotification(taskId, taskTitle)

            Log.d(TAG, "TaskReminderWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "TaskReminderWorker failed", e)
            Result.failure()
        }
    }

    private fun showNotification(taskId: Long, taskTitle: String) {
        Log.d(TAG, "Creating notification for task $taskId")

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use app icon instead of system icon for better reliability
        val notification = NotificationCompat.Builder(
            applicationContext,
            TaskTrackerApplication.TASK_REMINDER_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Better system icon
            .setContentTitle("Task Reminder")
            .setContentText("Due soon: $taskTitle")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Due soon: $taskTitle"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = applicationContext.getSystemService<NotificationManager>()
        Log.d(TAG, "Posting notification with ID: ${taskId.toInt()}")

        try {
            notificationManager?.notify(taskId.toInt(), notification)
            Log.d(TAG, "Notification posted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
        }
    }
}