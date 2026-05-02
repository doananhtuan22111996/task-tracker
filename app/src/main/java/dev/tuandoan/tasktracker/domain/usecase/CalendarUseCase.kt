package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.data.database.Task
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
 * `Map<LocalDate, DayDecoration>` for the v1.11.0 calendar surface (CAL-05, FR-03, FR-06..09).
 *
 * Emits reactively whenever tasks change. Each day's decoration reflects every task that
 * falls on that day — both persisted rows in [`ITaskRepository.observeTasksInRange`] and
 * projected occurrences computed by [RecurrenceCalculator.projectOccurrences] for recurring
 * parents whose window extends into the visible month.
 *
 * `hasRecurringProjection` is set **only** when a day's content comes solely from a
 * projected occurrence — a concrete row (even if it originated from recurrence regeneration)
 * takes precedence because the UI can act on it directly.
 *
 * Days with no content are absent from the map; callers treat a missing entry as "no tasks".
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

        // 2) Recurrence projections — add to days that the rule produces inside the window.
        //    Project ONLY from parent rows (parentRecurringTaskId == null). Generated children
        //    inherit the parent's recurrence fields (see TaskManager.buildNextTask), but the
        //    parent's `dueAt` remains the rule's anchor. Projecting from children would
        //    double-count already-concrete children and triple-count when the parent is also
        //    considered.
        //
        //    Concrete dates occupied by this chain (parent + its children inside the window)
        //    are removed from the projection to avoid double-counting.
        val chainConcreteDatesByRoot: Map<Long, Set<LocalDate>> = concreteInRange
            .filter { it.dueAt != null }
            .groupBy { it.parentRecurringTaskId ?: it.id }
            .mapValues { (_, tasks) ->
                tasks.mapTo(mutableSetOf()) {
                    // Safe: null dueAt filtered out above; smart-cast doesn't carry through
                    // the collection chain.
                    Instant.ofEpochMilli(it.dueAt!!).atZone(zone).toLocalDate()
                }
            }

        for (task in allActiveTasks) {
            if (task.isArchived) continue
            if (task.parentRecurringTaskId != null) continue // child — rule lives on the parent
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
            // Projection flag is meaningful only when the day has no concrete content —
            // the UI uses it to render a "projected" hint on otherwise-empty days.
            hasRecurringProjection = hasProjection && !hasConcrete,
        )
    }
}
