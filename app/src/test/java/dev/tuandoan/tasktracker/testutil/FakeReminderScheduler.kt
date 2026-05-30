package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.domain.scheduler.TaskReminderScheduler

/**
 * Fake implementation of [TaskReminderScheduler] for unit tests.
 * Tracks scheduled and cancelled reminders for assertion without touching OS APIs.
 */
open class FakeReminderScheduler : TaskReminderScheduler {

    data class ScheduledReminder(val taskId: Long, val title: String, val dueAt: Long, val offsetMinutes: Int)

    private val _scheduled = mutableListOf<ScheduledReminder>()
    private val _cancelled = mutableListOf<Long>()

    /** All reminders that were successfully scheduled */
    val scheduledReminders: List<ScheduledReminder> get() = _scheduled.toList()

    /** All task IDs whose reminders were cancelled */
    val cancelledTaskIds: List<Long> get() = _cancelled.toList()

    /** Control whether schedule returns true or false */
    var shouldScheduleSucceed: Boolean = true

    /** Fixed "current time" for deterministic reminder-time checks. Defaults to 0 (always succeeds). */
    var currentTimeMillis: Long = 0L

    override suspend fun schedule(taskId: Long, title: String, dueAt: Long, offsetMinutes: Int): Boolean {
        if (!shouldScheduleSucceed) return false
        // Simulate the real check: reminder time must be in the future
        val reminderTime = dueAt - (offsetMinutes * 60 * 1000L)
        if (currentTimeMillis > 0 && reminderTime <= currentTimeMillis) return false

        _scheduled.add(ScheduledReminder(taskId, title, dueAt, offsetMinutes))
        return true
    }

    override suspend fun cancel(taskId: Long) {
        _cancelled.add(taskId)
    }

    /** Check if a task has a scheduled reminder that hasn't been cancelled */
    fun isScheduled(taskId: Long): Boolean {
        val wasScheduled = _scheduled.any { it.taskId == taskId }
        val wasCancelled = _cancelled.contains(taskId)
        return wasScheduled && !wasCancelled
    }

    fun reset() {
        _scheduled.clear()
        _cancelled.clear()
        shouldScheduleSucceed = true
        currentTimeMillis = 0L
    }
}
