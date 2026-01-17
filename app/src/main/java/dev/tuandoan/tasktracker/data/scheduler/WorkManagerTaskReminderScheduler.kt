package dev.tuandoan.tasktracker.data.scheduler

import android.util.Log
import androidx.work.*
import dev.tuandoan.tasktracker.domain.scheduler.TaskReminderScheduler
import dev.tuandoan.tasktracker.work.TaskReminderWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerTaskReminderScheduler @Inject constructor(private val workManager: WorkManager) :
    TaskReminderScheduler {

    companion object {
        private const val TAG = "TaskReminder"
        private const val MIN_DELAY_MS = 1000L // Minimum 1 second delay to ensure scheduling works
    }

    override suspend fun schedule(taskId: Long, title: String, dueAt: Long, offsetMinutes: Int): Boolean {
        val reminderTimeMillis = dueAt - (offsetMinutes * 60 * 1000L)
        val currentTimeMillis = System.currentTimeMillis()

        Log.d(TAG, "Scheduling reminder for task $taskId: '$title'")
        Log.d(TAG, "Due at: $dueAt, offset: ${offsetMinutes}min, reminder time: $reminderTimeMillis")
        Log.d(TAG, "Current time: $currentTimeMillis")

        val delay = reminderTimeMillis - currentTimeMillis

        // Check if reminder time is too far in the past (more than 30 seconds)
        if (delay < -30000L) {
            Log.w(TAG, "Reminder time is too far in the past (${delay}ms)! Not scheduling.")
            return false
        }

        // Use minimum delay if reminder time is very close or slightly in the past
        val actualDelay = maxOf(delay, MIN_DELAY_MS)
        Log.d(TAG, "Scheduling with delay: ${delay}ms, actual delay: ${actualDelay}ms (${actualDelay / 1000}s)")

        val inputData = Data.Builder()
            .putLong(TaskReminderWorker.KEY_TASK_ID, taskId)
            .putString(TaskReminderWorker.KEY_TASK_TITLE, title)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(actualDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        val workName = getUniqueWorkName(taskId)
        Log.d(TAG, "Enqueuing work with name: $workName")

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        Log.d(TAG, "Reminder scheduled successfully for task $taskId")
        return true
    }

    override suspend fun cancel(taskId: Long) {
        val workName = getUniqueWorkName(taskId)
        Log.d(TAG, "Cancelling reminder for task $taskId (work name: $workName)")
        workManager.cancelUniqueWork(workName)
    }

    private fun getUniqueWorkName(taskId: Long): String = "task_reminder_$taskId"
}
