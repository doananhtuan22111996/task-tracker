package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import dev.tuandoan.tasktracker.domain.service.RecurrenceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    /**
     * Live list of [AgendaItem]s for [day] (CAL-23 part 2). Merges persisted concrete rows
     * with projected recurrence occurrences so the agenda sheet has something to render on
     * every day a dot will appear on post-CAL-24.
     *
     * Ordering: concrete rows first (ascending by `dueAt`, same as [observeTasksForDay]),
     * then projected occurrences (sorted by `parentTaskId` for deterministic test output).
     *
     * Dedup: if a concrete row already exists for a recurrence chain on [day], the chain's
     * projection for [day] is suppressed — concrete wins. Prevents the "chain doubles up on
     * its own regeneration day" bug CAL-05 originally solved for decorations; same principle
     * applies here.
     *
     * Not yet consumed by any screen — CAL-24 wires this into [CalendarViewModel] and
     * replaces the existing `observeTasksForDay` call. Kept additive in this PR so the
     * agenda UX doesn't regress while the UI change is in review.
     */
    fun observeAgendaForDay(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<AgendaItem>> {
        val startMillis = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return combine(
            taskRepository.observeTasksInRange(startMillis, endMillis),
            taskRepository.getAllTasks(),
        ) { concreteOnDay, allActive ->
            buildAgendaForDay(concreteOnDay, allActive, day, zone)
        }
    }

    private fun buildAgendaForDay(
        concreteOnDay: List<Task>,
        allActiveTasks: List<Task>,
        day: LocalDate,
        zone: ZoneId,
    ): List<AgendaItem> {
        val concreteItems = concreteOnDay.map { task -> AgendaItem.Concrete(task = task, date = day) }

        // Root ids of chains that already have a concrete row on [day]. Those chains must
        // not produce a Projected item, otherwise the agenda shows two rows for what the
        // user perceives as "today's instance of a repeating task".
        val concreteChainRoots: Set<Long> = concreteOnDay
            .map { it.parentRecurringTaskId ?: it.id }
            .toSet()

        val projectedItems = buildList {
            for (task in allActiveTasks) {
                if (task.isArchived) continue
                // Project only from chain roots. Generated children inherit the parent's
                // recurrence fields (see TaskManager.buildNextTask), so projecting from them
                // would re-enumerate dates the root already covers.
                if (task.parentRecurringTaskId != null) continue
                if (RecurrenceType.fromValue(task.recurrenceType) == RecurrenceType.NONE) continue
                if (task.id in concreteChainRoots) continue

                // Query the projection for a single-day window. Reuses CAL-07's
                // safety caps (maxOccurrences, fast-forward bound).
                val hits = RecurrenceCalculator.projectOccurrences(
                    task = task,
                    windowStart = day,
                    windowEnd = day,
                    zone = zone,
                )
                if (hits.isEmpty()) continue

                add(
                    AgendaItem.Projected(
                        parentTaskId = task.id,
                        date = day,
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        tag = task.tag,
                        tagColor = task.tagColor,
                        dueAtHasTime = task.dueAtHasTime,
                        reminderOffsetMinutes = task.reminderOffsetMinutes,
                        recurrenceType = task.recurrenceType,
                        recurrenceInterval = task.recurrenceInterval,
                        recurrenceDaysOfWeek = task.recurrenceDaysOfWeek,
                        recurrenceEndDate = task.recurrenceEndDate,
                    ),
                )
            }
        }.sortedBy { it.parentTaskId } // deterministic test output; UI will sort visually

        return concreteItems + projectedItems
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
