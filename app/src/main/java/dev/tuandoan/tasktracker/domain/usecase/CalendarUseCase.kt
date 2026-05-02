package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates concrete dated tasks into a `Map<LocalDate, DayDecoration>` for the v1.11.0
 * calendar month grid (CAL-05, FR-03, FR-06..09).
 *
 * Emits reactively whenever tasks change. Each day's decoration reflects persisted Room
 * rows whose `dueAt` falls on that day (from [`ITaskRepository.observeTasksInRange`]).
 *
 * Recurrence projections are intentionally NOT included — prior to CAL-37 the grid also
 * fed projected occurrences into the decoration, which produced "dots but empty agenda"
 * because [observeTasksForDay] only surfaces concrete rows. Materializing projections so
 * they render in both places is tracked as ADR-002 (CAL-23) + CAL-24.
 *
 * Days with no concrete tasks are absent from the map; callers treat a missing entry as
 * "no tasks".
 */
@Singleton
class CalendarUseCase @Inject constructor(private val taskRepository: ITaskRepository) {

    fun observeMonthDecorations(
        monthStart: LocalDate,
        monthEnd: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<Map<LocalDate, DayDecoration>> {
        val startMillis = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()
        // Exclusive end: observeTasksInRange uses a half-open window, so we advance one day.
        val endMillis = monthEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return taskRepository.observeTasksInRange(startMillis, endMillis)
            .map { concreteInRange -> buildDecorations(concreteInRange, zone) }
    }

    /**
     * Live list of concrete tasks whose `dueAt` falls on [day] (in [zone]). Used by the
     * day-agenda bottom sheet (CAL-17). Ordered by `dueAt` ascending (matches
     * [observeTasksInRange]'s order from CAL-06). Excludes archived. Includes completed.
     *
     * Projected recurrence occurrences for [day] are NOT included — the agenda currently
     * surfaces persisted Room rows only. Materialization of projections is tracked as
     * ADR-002 (CAL-23).
     */
    fun observeTasksForDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<Task>> {
        val startMillis = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return taskRepository.observeTasksInRange(startMillis, endMillis)
    }

    private fun buildDecorations(concreteInRange: List<Task>, zone: ZoneId): Map<LocalDate, DayDecoration> {
        val buckets = mutableMapOf<LocalDate, DayBucket>()
        for (task in concreteInRange) {
            val dueAt = task.dueAt ?: continue
            val date = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
            buckets.getOrPut(date) { DayBucket() }.addConcrete(task)
        }
        return buckets.mapValues { (date, bucket) -> bucket.toDecoration(date) }
    }

    /** Mutable accumulator used only inside [buildDecorations]. */
    private class DayBucket {
        private var taskCount: Int = 0
        private var completedCount: Int = 0
        private val priorityBuckets: MutableSet<Int> = mutableSetOf()

        fun addConcrete(task: Task) {
            taskCount++
            if (task.isCompleted) completedCount++
            priorityBuckets.add(task.priority)
        }

        fun toDecoration(date: LocalDate): DayDecoration = DayDecoration(
            date = date,
            taskCount = taskCount,
            priorityBuckets = priorityBuckets.toSet(),
            completedCount = completedCount,
            // Always false until CAL-23/24 materialize projections into the agenda.
            hasRecurringProjection = false,
        )
    }
}
