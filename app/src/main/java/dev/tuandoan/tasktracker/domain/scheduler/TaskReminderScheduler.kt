package dev.tuandoan.tasktracker.domain.scheduler

/**
 * Interface for scheduling and canceling task reminders.
 */
interface TaskReminderScheduler {
    /**
     * Schedule a reminder notification for a task.
     * @param taskId The ID of the task
     * @param title The title of the task to display in notification
     * @param dueAt The due date in epoch millis
     * @param offsetMinutes Minutes before due date to show reminder
     * @return true if scheduled successfully, false if invalid timing
     */
    suspend fun schedule(taskId: Long, title: String, dueAt: Long, offsetMinutes: Int): Boolean

    /**
     * Cancel any existing reminder for a task.
     * @param taskId The ID of the task
     */
    suspend fun cancel(taskId: Long)
}