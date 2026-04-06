package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.domain.model.StreakStats
import dev.tuandoan.tasktracker.domain.model.TaskStreak
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import dev.tuandoan.tasktracker.domain.service.StreakCalculator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates streak calculation across all recurring task chains.
 * Fetches active recurring root IDs, computes streaks via [StreakCalculator],
 * and returns aggregated [StreakStats].
 */
@Singleton
class StreakUseCase @Inject constructor(private val repository: ITaskRepository) {

    suspend fun getStreakStats(): StreakStats {
        val rootIds = repository.getActiveRecurringRootIds()

        if (rootIds.isEmpty()) {
            return StreakStats(
                activeRecurringCount = 0,
                bestCurrentStreak = null,
                allTimeBestStreak = null,
            )
        }

        val allCompleted = repository.getCompletedTasksForChains(rootIds)
        val grouped = allCompleted.groupBy { it.parentRecurringTaskId ?: it.id }

        var bestCurrent: TaskStreak? = null
        var allTimeBest: TaskStreak? = null

        for (rootId in rootIds) {
            val completedTasks = grouped[rootId] ?: continue

            val result = StreakCalculator.calculate(completedTasks)
            val representativeTask = completedTasks.last()
            val taskStreak = TaskStreak(
                taskTitle = representativeTask.title,
                taskId = rootId,
                currentStreak = result.currentStreak,
                longestStreak = result.longestStreak,
            )

            if (bestCurrent == null || taskStreak.currentStreak > bestCurrent.currentStreak) {
                bestCurrent = taskStreak
            }
            if (allTimeBest == null || taskStreak.longestStreak > allTimeBest.longestStreak) {
                allTimeBest = taskStreak
            }
        }

        return StreakStats(
            activeRecurringCount = rootIds.size,
            bestCurrentStreak = bestCurrent,
            allTimeBestStreak = allTimeBest,
        )
    }

    /**
     * Returns a map of rootChainId → currentStreak count for badge display.
     * Only includes chains with currentStreak >= 2.
     */
    suspend fun getStreakMap(): Map<Long, Int> {
        val rootIds = repository.getActiveRecurringRootIds()
        if (rootIds.isEmpty()) return emptyMap()

        val allCompleted = repository.getCompletedTasksForChains(rootIds)
        val grouped = allCompleted.groupBy { it.parentRecurringTaskId ?: it.id }

        val map = mutableMapOf<Long, Int>()
        for (rootId in rootIds) {
            val completedTasks = grouped[rootId] ?: continue
            val result = StreakCalculator.calculate(completedTasks)
            if (result.currentStreak >= 2) {
                map[rootId] = result.currentStreak
            }
        }
        return map
    }
}
