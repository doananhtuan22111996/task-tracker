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
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val breadcrumbLogger: BreadcrumbLogger,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        private const val TAG = "TaskReminder"
        private const val SNOOZE_15_REQUEST_CODE_OFFSET = 100_000
        private const val SNOOZE_1H_REQUEST_CODE_OFFSET = 200_000
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "TaskReminderWorker.doWork() started")

        return try {
            val taskId = inputData.getLong(KEY_TASK_ID, -1)
            val taskTitle = inputData.getString(KEY_TASK_TITLE)
                ?: applicationContext.getString(R.string.notification_default_task)

            Log.d(TAG, "Processing reminder for task $taskId: '$taskTitle'")

            if (taskId == -1L) {
                Log.e(TAG, "Invalid task ID: $taskId")
                // FB-12: CRITICAL — KEY_TASK_TITLE holds the user's task title. NEVER include
                // `taskTitle` in any breadcrumb. id-only is the contract.
                breadcrumbLogger.log(BreadcrumbCategory.REMINDER, "fired invalid_id")
                return Result.failure()
            }

            // FB-12: see caveat above — id is opaque, title is not.
            breadcrumbLogger.log(BreadcrumbCategory.REMINDER, "fired id=$taskId")

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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Mark Complete" action intent
        val completeIntent = Intent(applicationContext, TaskCompleteReceiver::class.java).apply {
            action = TaskCompleteReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskCompleteReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskCompleteReceiver.EXTRA_NOTIFICATION_ID, taskId.toInt())
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            taskId.toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Snooze 15 min" action
        val snooze15Intent = Intent(applicationContext, TaskSnoozeReceiver::class.java).apply {
            action = TaskSnoozeReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskSnoozeReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskSnoozeReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(TaskSnoozeReceiver.EXTRA_NOTIFICATION_ID, taskId.toInt())
            putExtra(TaskSnoozeReceiver.EXTRA_SNOOZE_DELAY_MS, SnoozeDelayCalculator.SNOOZE_15_MIN_MS)
        }
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            taskId.toInt() + SNOOZE_15_REQUEST_CODE_OFFSET,
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Snooze 1 hour" action
        val snooze1hIntent = Intent(applicationContext, TaskSnoozeReceiver::class.java).apply {
            action = TaskSnoozeReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskSnoozeReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskSnoozeReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(TaskSnoozeReceiver.EXTRA_NOTIFICATION_ID, taskId.toInt())
            putExtra(TaskSnoozeReceiver.EXTRA_SNOOZE_DELAY_MS, SnoozeDelayCalculator.SNOOZE_1_HOUR_MS)
        }
        val snooze1hPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            taskId.toInt() + SNOOZE_1H_REQUEST_CODE_OFFSET,
            snooze1hIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            TaskTrackerApplication.TASK_REMINDER_CHANNEL_ID,
        )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(applicationContext.getString(R.string.notification_due_soon, taskTitle))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    applicationContext.getString(R.string.notification_due_soon, taskTitle),
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                applicationContext.getString(R.string.notification_action_complete),
                completePendingIntent,
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                applicationContext.getString(R.string.notification_snooze_15min),
                snooze15PendingIntent,
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                applicationContext.getString(R.string.notification_snooze_1hour),
                snooze1hPendingIntent,
            )
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
