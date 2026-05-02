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
 * `hasRecurringProjection` is a forward-looking flag for ADR-002 (CAL-23 + CAL-24) —
 * it will mark days whose only content is a projected recurrence occurrence (no DB row).
 * Currently always `false`: before CAL-37, projections were fed into the grid even though
 * the agenda didn't render them, producing "dots but empty sheet". Once CAL-23/24 land
 * (materialize projections into the agenda), this flag flips back on.
 *
 * A day with `taskCount == 0` is not emitted — callers treat a missing entry as "no tasks".
 */
data class DayDecoration(
    val date: LocalDate,
    val taskCount: Int = 0,
    val priorityBuckets: Set<Int> = emptySet(),
    val completedCount: Int = 0,
    val hasRecurringProjection: Boolean = false,
)
