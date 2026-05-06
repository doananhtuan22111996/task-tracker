package dev.tuandoan.tasktracker.domain.model

import androidx.compose.runtime.Immutable
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
 * `hasRecurringProjection` is `true` when every task on [date] is a projected recurrence
 * occurrence (no DB row yet). Used by the day agenda to render a distinct "upcoming"
 * visual for rows that will be materialized on tap (ADR-002, CAL-23 + CAL-24). When
 * the day has at least one concrete row, this flag is `false` — concrete always wins.
 *
 * A day with `taskCount == 0` is not emitted — callers treat a missing entry as "no tasks".
 *
 * Marked [@Immutable][androidx.compose.runtime.Immutable] for CAL-31 so Compose can skip
 * `DayCell` recomposition when the instance is structurally equal to the previous value.
 * Without the annotation, Compose conservatively treats the embedded `Set<Int>` as unstable
 * and re-runs every cell on every `decorations` map emission, even for days whose decoration
 * is unchanged. The contract: instances are never mutated after construction; the builder
 * inside `CalendarUseCase.buildDecorations` produces a fresh instance per emission and
 * `Set<Int>` is a read-only `toSet()` snapshot.
 *
 * Note: the annotation introduces a compile-time dependency from `domain/model` on
 * `androidx.compose.runtime`. Acceptable in this single-module app; if the project later
 * splits `:app`/`:domain`, move this via a Compose stability-configuration file to keep the
 * domain layer compose-free.
 */
@Immutable
data class DayDecoration(
    val date: LocalDate,
    val taskCount: Int = 0,
    val priorityBuckets: Set<Int> = emptySet(),
    val completedCount: Int = 0,
    val hasRecurringProjection: Boolean = false,
)
