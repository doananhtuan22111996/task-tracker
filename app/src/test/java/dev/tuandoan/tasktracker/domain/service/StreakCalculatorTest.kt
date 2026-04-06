package dev.tuandoan.tasktracker.domain.service

import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.TestTaskFactory.ONE_DAY_MS
import dev.tuandoan.tasktracker.testutil.TestTaskFactory.ONE_HOUR_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    // Helper: create a completed recurring task with due and completion timestamps
    private fun completedRecurring(id: Long, dueAt: Long, completedAt: Long, parentRecurringTaskId: Long = 100L) =
        TestTaskFactory.createTask(
            id = id,
            title = "Recurring Task",
            isCompleted = true,
            dueAt = dueAt,
            completedAt = completedAt,
            recurrenceType = 1, // DAILY
            parentRecurringTaskId = parentRecurringTaskId,
        )

    @Test
    fun `empty list returns zero streaks`() {
        val result = StreakCalculator.calculate(emptyList())
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `single on-time completion returns streak of 1`() {
        val tasks = listOf(
            completedRecurring(
                id = 1,
                dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS,
                completedAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `completion exactly at due time counts as on-time`() {
        val dueAt = TestTaskFactory.BASE_TIMESTAMP + ONE_DAY_MS
        val tasks = listOf(
            completedRecurring(id = 1, dueAt = dueAt, completedAt = dueAt),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `consecutive on-time completions build streak`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            completedRecurring(
                id = 2,
                dueAt = base + 2 * ONE_DAY_MS,
                completedAt = base + 2 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `overdue completion breaks the streak`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            completedRecurring(
                id = 2,
                dueAt = base + 2 * ONE_DAY_MS,
                completedAt = base + 2 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            // Overdue: completed 2 hours after due
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt =
                base + 3 * ONE_DAY_MS + 2 * ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 4,
                dueAt = base + 4 * ONE_DAY_MS,
                completedAt = base + 4 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(1, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `streak resumes after break and longest is preserved`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            // First streak of 3
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            completedRecurring(
                id = 2,
                dueAt = base + 2 * ONE_DAY_MS,
                completedAt = base + 2 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            // Break
            completedRecurring(
                id = 4,
                dueAt = base + 4 * ONE_DAY_MS,
                completedAt = base + 4 * ONE_DAY_MS + ONE_HOUR_MS,
            ),
            // New streak of 2
            completedRecurring(
                id = 5,
                dueAt = base + 5 * ONE_DAY_MS,
                completedAt = base + 5 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 6,
                dueAt = base + 6 * ONE_DAY_MS,
                completedAt = base + 6 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(2, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `task without dueAt is skipped`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            // No dueAt — skipped
            TestTaskFactory.createTask(
                id = 2,
                isCompleted = true,
                completedAt = base + 2 * ONE_DAY_MS,
                dueAt = null,
                recurrenceType = 1,
                parentRecurringTaskId = 100L,
            ),
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `task without completedAt is skipped`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            // No completedAt — skipped
            TestTaskFactory.createTask(
                id = 2,
                isCompleted = true,
                completedAt = null,
                dueAt = base + 2 * ONE_DAY_MS,
                recurrenceType = 1,
                parentRecurringTaskId = 100L,
            ),
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `all overdue completions give zero current streak`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS + ONE_HOUR_MS),
            completedRecurring(
                id = 2,
                dueAt = base + 2 * ONE_DAY_MS,
                completedAt = base + 2 * ONE_DAY_MS + ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS + ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `longest streak is from earlier period not current`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            // Streak of 5
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            completedRecurring(
                id = 2,
                dueAt = base + 2 * ONE_DAY_MS,
                completedAt = base + 2 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 4,
                dueAt = base + 4 * ONE_DAY_MS,
                completedAt = base + 4 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 5,
                dueAt = base + 5 * ONE_DAY_MS,
                completedAt = base + 5 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            // Break
            completedRecurring(
                id = 6,
                dueAt = base + 6 * ONE_DAY_MS,
                completedAt = base + 6 * ONE_DAY_MS + ONE_HOUR_MS,
            ),
            // Current streak of 1
            completedRecurring(
                id = 7,
                dueAt = base + 7 * ONE_DAY_MS,
                completedAt = base + 7 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(1, result.currentStreak)
        assertEquals(5, result.longestStreak)
    }

    @Test
    fun `multiple breaks track all-time best correctly`() {
        val base = TestTaskFactory.BASE_TIMESTAMP
        val tasks = listOf(
            // Streak of 2
            completedRecurring(id = 1, dueAt = base + ONE_DAY_MS, completedAt = base + ONE_DAY_MS - ONE_HOUR_MS),
            completedRecurring(
                id = 2,
                dueAt = base + 2 * ONE_DAY_MS,
                completedAt = base + 2 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            // Break
            completedRecurring(
                id = 3,
                dueAt = base + 3 * ONE_DAY_MS,
                completedAt = base + 3 * ONE_DAY_MS + ONE_HOUR_MS,
            ),
            // Streak of 4 (longest)
            completedRecurring(
                id = 4,
                dueAt = base + 4 * ONE_DAY_MS,
                completedAt = base + 4 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 5,
                dueAt = base + 5 * ONE_DAY_MS,
                completedAt = base + 5 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 6,
                dueAt = base + 6 * ONE_DAY_MS,
                completedAt = base + 6 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            completedRecurring(
                id = 7,
                dueAt = base + 7 * ONE_DAY_MS,
                completedAt = base + 7 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
            // Break
            completedRecurring(
                id = 8,
                dueAt = base + 8 * ONE_DAY_MS,
                completedAt = base + 8 * ONE_DAY_MS + ONE_HOUR_MS,
            ),
            // Current streak of 1
            completedRecurring(
                id = 9,
                dueAt = base + 9 * ONE_DAY_MS,
                completedAt = base + 9 * ONE_DAY_MS - ONE_HOUR_MS,
            ),
        )
        val result = StreakCalculator.calculate(tasks)
        assertEquals(1, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }
}
