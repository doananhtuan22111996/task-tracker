package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.StreakResult

/**
 * Pure Kotlin utility for calculating completion streaks of recurring tasks.
 * No Android dependencies — fully unit-testable.
 *
 * A streak is a sequence of consecutive on-time completions where
 * completedAt <= dueAt for each task in the chain, ordered by completedAt ASC.
 */
object StreakCalculator {

    /**
     * Calculates the current and longest streak from a list of completed tasks
     * in a recurring chain, sorted by [Task.completedAt] ascending.
     *
     * Rules:
     * - A completion is "on-time" if completedAt <= dueAt
     * - An overdue completion (completedAt > dueAt) breaks the streak
     * - Tasks without dueAt or completedAt are skipped
     *
     * @param completedTasks tasks in the chain, sorted by completedAt ASC.
     *   Only completed tasks should be passed (isCompleted = true).
     * @return [StreakResult] with current and longest streak counts.
     */
    fun calculate(completedTasks: List<Task>): StreakResult {
        if (completedTasks.isEmpty()) return StreakResult(currentStreak = 0, longestStreak = 0)

        var currentStreak = 0
        var longestStreak = 0

        for (task in completedTasks) {
            val completedAt = task.completedAt ?: continue
            val dueAt = task.dueAt ?: continue

            if (completedAt <= dueAt) {
                currentStreak++
            } else {
                // Overdue completion breaks the streak
                longestStreak = maxOf(longestStreak, currentStreak)
                currentStreak = 0
            }
        }

        longestStreak = maxOf(longestStreak, currentStreak)

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
        )
    }
}
