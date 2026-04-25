package dev.tuandoan.tasktracker.data.database

/**
 * Aggregate projection of a task's subtask progress. Emitted by the
 * [SubtaskDao.observeSubtaskProgress] query; consumed by the UI to render the inline
 * "m / n" indicator on the task list row.
 *
 * Only tasks with at least one subtask appear in the stream — callers that look up by `taskId`
 * must treat a missing entry as "no subtasks".
 */
data class SubtaskProgress(val taskId: Long, val total: Int, val completed: Int)
