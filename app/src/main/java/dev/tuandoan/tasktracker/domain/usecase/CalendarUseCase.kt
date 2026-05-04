package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.AgendaItem
import dev.tuandoan.tasktracker.domain.model.DayDecoration
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import dev.tuandoan.tasktracker.domain.service.RecurrenceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates concrete dated tasks and projected recurrence occurrences into a
 * `Map<LocalDate, DayDecoration>` for the v1.11.0 calendar month grid (CAL-05, CAL-23, CAL-24).
 *
 * Emits reactively whenever tasks change. Each day's decoration reflects every task that
 * falls on that day — both persisted Room rows and projected occurrences from
 * [RecurrenceCalculator.projectOccurrences] for recurring parents whose window extends into
 * the visible month.
 *
 * `hasRecurringProjection` on [DayDecoration] is set only when a day's content comes solely
 * from a projected occurrence; a concrete row always takes precedence.
 *
 * Days with no content are absent from the map; callers treat a missing entry as "no tasks".
 *
 * History: CAL-37 temporarily reduced this to concrete-only to eliminate a "dots but empty
 * agenda" UX lie (observeTasksForDay returned concrete rows only). CAL-24 re-enables the
 * projection feed now that the agenda surfaces projections too via [observeAgendaForDay].
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

        return combine(
            taskRepository.observeTasksInRange(startMillis, endMillis),
            taskRepository.getAllTasks(),
        ) { concreteInRange, allActiveTasks ->
            buildDecorations(concreteInRange, allActiveTasks, monthStart, monthEnd, zone)
        }
    }

    /**
     * Live list of [AgendaItem]s for [day] (CAL-23 part 2). Merges persisted concrete rows
     * with projected recurrence occurrences so every day that carries a dot in the month
     * grid has something tappable in the agenda sheet.
     *
     * Ordering: concrete rows first (ascending by `dueAt`, matching
     * [ITaskRepository.observeTasksInRange]), then projected occurrences (sorted by
     * `parentTaskId` for deterministic output).
     *
     * Dedup: if a concrete row already exists for a recurrence chain on [day], the chain's
     * projection for [day] is suppressed — concrete wins.
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

        val concreteChainRoots: Set<Long> = concreteOnDay
            .map { it.parentRecurringTaskId ?: it.id }
            .toSet()

        val projectedItems = buildList {
            for (task in allActiveTasks) {
                if (task.isArchived) continue
                if (task.parentRecurringTaskId != null) continue
                if (RecurrenceType.fromValue(task.recurrenceType) == RecurrenceType.NONE) continue
                if (task.id in concreteChainRoots) continue

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
        }.sortedBy { it.parentTaskId }

        return concreteItems + projectedItems
    }

    private fun buildDecorations(
        concreteInRange: List<Task>,
        allActiveTasks: List<Task>,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        zone: ZoneId,
    ): Map<LocalDate, DayDecoration> {
        val buckets = mutableMapOf<LocalDate, DayBucket>()

        // 1) Concrete tasks — the authoritative source for each day they appear on.
        for (task in concreteInRange) {
            val dueAt = task.dueAt ?: continue
            val date = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
            buckets.getOrPut(date) { DayBucket() }.addConcrete(task)
        }

        // 2) Recurrence projections — enumerate only from chain roots. Concrete dates
        //    already claimed by the chain (parent + materialized children inside the window)
        //    are subtracted so a Monday with both the root and this-week's concrete child
        //    still counts as exactly one dot on that Monday.
        val chainConcreteDatesByRoot: Map<Long, Set<LocalDate>> = concreteInRange
            .filter { it.dueAt != null }
            .groupBy { it.parentRecurringTaskId ?: it.id }
            .mapValues { (_, tasks) ->
                tasks.mapTo(mutableSetOf()) { task ->
                    Instant.ofEpochMilli(task.dueAt!!).atZone(zone).toLocalDate()
                }
            }

        for (task in allActiveTasks) {
            if (task.isArchived) continue
            if (task.parentRecurringTaskId != null) continue
            if (RecurrenceType.fromValue(task.recurrenceType) == RecurrenceType.NONE) continue

            val projectedDates = RecurrenceCalculator.projectOccurrences(
                task = task,
                windowStart = monthStart,
                windowEnd = monthEnd,
                zone = zone,
            )
            if (projectedDates.isEmpty()) continue

            val concreteDatesForChain = chainConcreteDatesByRoot[task.id].orEmpty()
            for (date in projectedDates) {
                if (date in concreteDatesForChain) continue
                buckets.getOrPut(date) { DayBucket() }.addProjection(task)
            }
        }

        return buckets.mapValues { (date, bucket) -> bucket.toDecoration(date) }
    }

    /** Mutable accumulator used only inside [buildDecorations]. */
    private class DayBucket {
        private var taskCount: Int = 0
        private var completedCount: Int = 0
        private val priorityBuckets: MutableSet<Int> = mutableSetOf()
        private var hasConcrete: Boolean = false
        private var hasProjection: Boolean = false

        fun addConcrete(task: Task) {
            taskCount++
            if (task.isCompleted) completedCount++
            priorityBuckets.add(task.priority)
            hasConcrete = true
        }

        fun addProjection(task: Task) {
            taskCount++
            priorityBuckets.add(task.priority)
            hasProjection = true
        }

        fun toDecoration(date: LocalDate): DayDecoration = DayDecoration(
            date = date,
            taskCount = taskCount,
            priorityBuckets = priorityBuckets.toSet(),
            completedCount = completedCount,
            // true only when the day's content is exclusively projection; concrete wins.
            hasRecurringProjection = hasProjection && !hasConcrete,
        )
    }
}
