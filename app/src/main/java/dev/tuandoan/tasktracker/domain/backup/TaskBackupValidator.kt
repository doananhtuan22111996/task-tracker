package dev.tuandoan.tasktracker.domain.backup

import dev.tuandoan.tasktracker.data.database.Task
import javax.inject.Inject

/**
 * Implementation of [BackupValidator] that applies sanitization rules to imported tasks.
 *
 * Rules:
 * - Skip tasks with blank titles.
 * - Truncate title to [MAX_TITLE_LENGTH] characters.
 * - Truncate description to [MAX_DESCRIPTION_LENGTH] characters.
 * - Clamp priority to [0, 2].
 * - Default createdAt to current time if <= 0.
 * - Set completedAt = null if isCompleted = false.
 * - Set archivedAt = null if isArchived = false.
 */
class TaskBackupValidator @Inject constructor() : BackupValidator {

    override fun validate(tasks: List<Task>): BackupValidator.ValidationResult {
        val validTasks = mutableListOf<Task>()
        var skipped = 0

        for (task in tasks) {
            if (task.title.isBlank()) {
                skipped++
                continue
            }

            val sanitized = task.copy(
                title = task.title.take(MAX_TITLE_LENGTH),
                description = task.description.take(MAX_DESCRIPTION_LENGTH),
                priority = task.priority.coerceIn(MIN_PRIORITY, MAX_PRIORITY),
                createdAt = if (task.createdAt <= 0L) System.currentTimeMillis() else task.createdAt,
                completedAt = if (!task.isCompleted) null else task.completedAt,
                archivedAt = if (!task.isArchived) null else task.archivedAt,
                recurrenceType = task.recurrenceType.coerceIn(MIN_RECURRENCE_TYPE, MAX_RECURRENCE_TYPE),
                recurrenceInterval = task.recurrenceInterval.coerceAtLeast(MIN_RECURRENCE_INTERVAL),
                recurrenceDaysOfWeek = task.recurrenceDaysOfWeek and MAX_DAYS_OF_WEEK_BITMASK,
            )
            validTasks.add(sanitized)
        }

        return BackupValidator.ValidationResult(
            validTasks = validTasks,
            skippedCount = skipped,
        )
    }

    companion object {
        private const val MIN_PRIORITY = 0
        private const val MAX_PRIORITY = 2
        private const val MAX_TITLE_LENGTH = 100
        private const val MAX_DESCRIPTION_LENGTH = 500
        private const val MIN_RECURRENCE_TYPE = 0
        private const val MAX_RECURRENCE_TYPE = 4
        private const val MIN_RECURRENCE_INTERVAL = 1
        private const val MAX_DAYS_OF_WEEK_BITMASK = 0x7F // Mon-Sun = bits 0-6
    }
}
