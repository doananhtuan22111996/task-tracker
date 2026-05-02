package dev.tuandoan.tasktracker.domain.model

import java.time.LocalDate

/**
 * Per-day aggregate that drives the calendar month grid cell renderer (CAL-12).
 * Computed by [dev.tuandoan.tasktracker.domain.usecase.CalendarUseCase] from tasks
 * whose `dueAt` falls on [date], plus recurrence projections for that date.
 *
 * `priorityBuckets` holds the distinct priority ints present on [date] (0 = LOW,
 * 1 = MEDIUM, 2 = HIGH). The cell renders one dot per bucket; overflow beyond three
 * is shown as `+N`.
 *
 * `hasRecurringProjection` is true when at least one task on [date] is a projected
 * recurrence occurrence (no DB row yet). Drives the small "recurring" hint in the
 * agenda per ADR-002 (CAL-23).
 *
 * A day with `taskCount == 0` and `hasRecurringProjection == false` is not emitted —
 * callers treat a missing entry as "no tasks".
 */
data class DayDecoration(
    val date: LocalDate,
    val taskCount: Int = 0,
    val priorityBuckets: Set<Int> = emptySet(),
    val completedCount: Int = 0,
    val hasRecurringProjection: Boolean = false,
)
