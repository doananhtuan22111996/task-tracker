package dev.tuandoan.tasktracker.domain.model

import dev.tuandoan.tasktracker.data.database.Task
import java.time.LocalDate

/**
 * A single row rendered in the calendar day-agenda sheet (ADR-002 / CAL-23).
 *
 * Two kinds:
 * - [Concrete] — backed by a persisted `tasks` row. All normal edit/complete/archive/pin actions
 *   apply directly to `task.id`.
 * - [Projected] — a recurrence occurrence predicted by [RecurrenceType]'s rule but not yet
 *   persisted. Any user interaction materializes it into a concrete row first
 *   (see [dev.tuandoan.tasktracker.domain.ITaskManager.materializeProjectedOccurrence]) and
 *   then the normal concrete-row handler runs.
 *
 * Kept as a sealed interface (not a data class) because [Concrete] wraps a full [Task] while
 * [Projected] carries only the fields needed to render a row; coercing them into a common
 * record would either bloat [Projected] with nullable task-only columns or strip useful detail
 * off [Concrete].
 */
sealed interface AgendaItem {
    /** Calendar date this row renders on, in the user's zone. */
    val date: LocalDate

    data class Concrete(val task: Task, override val date: LocalDate) : AgendaItem

    data class Projected(
        val parentTaskId: Long,
        override val date: LocalDate,
        val title: String,
        val description: String,
        val priority: Int,
        val tag: String?,
        val tagColor: String?,
        val dueAtHasTime: Boolean,
        val reminderOffsetMinutes: Int?,
        val recurrenceType: Int,
        val recurrenceInterval: Int,
        val recurrenceDaysOfWeek: Int,
        val recurrenceEndDate: Long?,
    ) : AgendaItem
}
