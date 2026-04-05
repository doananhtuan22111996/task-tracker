package dev.tuandoan.tasktracker.work

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.ITaskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * BroadcastReceiver that handles "Mark Complete" action from task reminder notifications.
 * Completes the task (including recurrence handling) and dismisses the notification.
 */
@AndroidEntryPoint
class TaskCompleteReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "dev.tuandoan.tasktracker.ACTION_COMPLETE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        private const val TAG = "TaskCompleteReceiver"
    }

    @Inject
    lateinit var taskManager: ITaskManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE_TASK) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (taskId == -1L) return

        // Dismiss the notification immediately
        if (notificationId != -1) {
            context.getSystemService<NotificationManager>()?.cancel(notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskManager.getTaskById(taskId)
                if (task != null && !task.isCompleted) {
                    taskManager.toggleTaskCompletion(task)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.notification_task_completed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete task $taskId from notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
