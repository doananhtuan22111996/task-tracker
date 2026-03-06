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
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.TaskTrackerApplication
import dev.tuandoan.tasktracker.data.database.TaskDao

@HiltWorker
class OverdueDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "OverdueDigestWorker.doWork() started")

        return try {
            val overdue = taskDao.getOverdueTasks(System.currentTimeMillis())
            if (overdue.isEmpty()) {
                Log.d(TAG, "No overdue tasks found")
                return Result.success()
            }

            // Check notification permission
            if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                Log.w(TAG, "Notifications disabled, skipping digest")
                return Result.success()
            }

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                OVERDUE_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val taskWord = if (overdue.size > 1) "tasks" else "task"
            val notification = NotificationCompat.Builder(
                applicationContext,
                TaskTrackerApplication.TASK_REMINDER_CHANNEL_ID,
            )
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(applicationContext.getString(R.string.notification_overdue_title))
                .setContentText("You have ${overdue.size} overdue $taskWord")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = applicationContext.getSystemService<NotificationManager>()
            notificationManager?.notify(OVERDUE_NOTIFICATION_ID, notification)

            Log.d(TAG, "Overdue digest notification posted for ${overdue.size} tasks")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "OverdueDigestWorker failed", e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "overdue_digest"
        private const val OVERDUE_NOTIFICATION_ID = 9999
        private const val TAG = "OverdueDigest"
    }
}
