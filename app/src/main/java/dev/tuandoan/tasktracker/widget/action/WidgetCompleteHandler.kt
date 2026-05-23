package dev.tuandoan.tasktracker.widget.action

import android.util.Log
import dev.tuandoan.tasktracker.domain.ITaskManager

/**
 * Pure-Kotlin handler for the widget's complete-from-row action. Lifted out of
 * the Glance [ActionCallback] so the cancel-reminder → mark-complete → notify
 * ordering (NFR-08) and idempotency under double-tap (FR-12) are JVM-unit-testable
 * without the Glance runtime.
 *
 * Ordering is inherited from [ITaskManager.toggleTaskCompletion]: it cancels the
 * reminder before persisting completion and triggers the widget update at the end.
 * This handler keeps the widget surface free of business logic and reuses the same
 * code path as the in-app and notification "Mark Complete" flows.
 */
class WidgetCompleteHandler(private val taskManager: ITaskManager) {

    /**
     * Look up [taskId] and complete it if it is active. No-op on missing or
     * already-completed tasks — second call from a rapid double-tap is silently
     * absorbed (FR-12).
     *
     * @return true if a completion was applied, false on no-op (missing / already done).
     */
    suspend fun complete(taskId: Long): Boolean {
        if (taskId <= 0L) return false
        return try {
            val task = taskManager.getTaskById(taskId) ?: return false
            if (task.isCompleted) return false
            taskManager.toggleTaskCompletion(task)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete task $taskId from widget", e)
            false
        }
    }

    companion object {
        private const val TAG = "WidgetCompleteHandler"
    }
}
