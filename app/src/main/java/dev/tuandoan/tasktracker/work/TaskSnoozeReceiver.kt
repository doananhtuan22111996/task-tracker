package dev.tuandoan.tasktracker.work

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.tasktracker.R
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class TaskSnoozeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE_TASK = "dev.tuandoan.tasktracker.ACTION_SNOOZE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SNOOZE_DELAY_MS = "extra_snooze_delay_ms"
        private const val TAG = "TaskSnoozeReceiver"
    }

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE_TASK) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)
            ?: context.getString(R.string.notification_default_task)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val delayMs = intent.getLongExtra(EXTRA_SNOOZE_DELAY_MS, SnoozeDelayCalculator.SNOOZE_15_MIN_MS)

        if (taskId == -1L) return

        if (notificationId != -1) {
            context.getSystemService<NotificationManager>()?.cancel(notificationId)
        }

        val inputData = Data.Builder()
            .putLong(TaskReminderWorker.KEY_TASK_ID, taskId)
            .putString(TaskReminderWorker.KEY_TASK_TITLE, taskTitle)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "task_reminder_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        Log.d(TAG, "Snoozed task $taskId for ${delayMs / 1000}s")

        Toast.makeText(
            context,
            context.getString(R.string.notification_snoozed),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
