package dev.tuandoan.tasktracker.domain.usecase

import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.TestTaskFactory.BASE_TIMESTAMP
import dev.tuandoan.tasktracker.testutil.TestTaskFactory.ONE_DAY_MS
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StreakUseCaseTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var useCase: StreakUseCase

    @Before
    fun setup() {
        repository = FakeTaskRepository()
        useCase = StreakUseCase(repository)
    }

    // === getStreakStats ===

    @Test
    fun `getStreakStats returns empty when no recurring tasks`() = runTest {
        val stats = useCase.getStreakStats()
        assertEquals(0, stats.activeRecurringCount)
        assertNull(stats.bestCurrentStreak)
        assertNull(stats.allTimeBestStreak)
    }

    @Test
    fun `getStreakStats returns count with no streaks when no completions`() = runTest {
        // Root recurring task (active, not completed)
        repository.seed(
            TestTaskFactory.createTask(
                id = 1,
                recurrenceType = 1,
                recurrenceInterval = 1,
            ),
        )

        val stats = useCase.getStreakStats()
        assertEquals(1, stats.activeRecurringCount)
        assertNull(stats.bestCurrentStreak)
        assertNull(stats.allTimeBestStreak)
    }

    @Test
    fun `getStreakStats computes streak across chain`() = runTest {
        // Root task (active, recurring)
        val root = TestTaskFactory.createTask(
            id = 1,
            recurrenceType = 1,
            recurrenceInterval = 1,
        )
        // Completed child 1 — on time
        val child1 = TestTaskFactory.createTask(
            id = 2,
            title = "Daily Task",
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )
        // Completed child 2 — on time
        val child2 = TestTaskFactory.createTask(
            id = 3,
            title = "Daily Task",
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
        )
        repository.seed(root, child1, child2)

        val stats = useCase.getStreakStats()
        assertEquals(1, stats.activeRecurringCount)
        assertNotNull(stats.bestCurrentStreak)
        assertEquals(2, stats.bestCurrentStreak!!.currentStreak)
        assertEquals(1L, stats.bestCurrentStreak!!.taskId)
    }

    @Test
    fun `getStreakStats picks best streak across multiple chains`() = runTest {
        // Chain A: 2 on-time completions
        val rootA = TestTaskFactory.createTask(id = 1, recurrenceType = 1)
        val childA1 = TestTaskFactory.createTask(
            id = 10,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )
        val childA2 = TestTaskFactory.createTask(
            id = 11,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
        )

        // Chain B: 3 on-time completions
        val rootB = TestTaskFactory.createTask(id = 2, recurrenceType = 1)
        val childB1 = TestTaskFactory.createTask(
            id = 20,
            title = "Chain B",
            parentRecurringTaskId = 2,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )
        val childB2 = TestTaskFactory.createTask(
            id = 21,
            title = "Chain B",
            parentRecurringTaskId = 2,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
        )
        val childB3 = TestTaskFactory.createTask(
            id = 22,
            title = "Chain B",
            parentRecurringTaskId = 2,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 3 * ONE_DAY_MS,
        )

        repository.seed(rootA, childA1, childA2, rootB, childB1, childB2, childB3)

        val stats = useCase.getStreakStats()
        assertEquals(2, stats.activeRecurringCount)
        assertEquals(3, stats.bestCurrentStreak!!.currentStreak)
        assertEquals(2L, stats.bestCurrentStreak!!.taskId)
    }

    // === getStreakMap ===

    @Test
    fun `getStreakMap returns empty when no recurring tasks`() = runTest {
        val map = useCase.getStreakMap()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `getStreakMap excludes chains with streak below 2`() = runTest {
        val root = TestTaskFactory.createTask(id = 1, recurrenceType = 1)
        // Only 1 on-time completion → streak = 1
        val child = TestTaskFactory.createTask(
            id = 2,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )
        repository.seed(root, child)

        val map = useCase.getStreakMap()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `getStreakMap includes chains with streak 2 or more`() = runTest {
        val root = TestTaskFactory.createTask(id = 1, recurrenceType = 1)
        val child1 = TestTaskFactory.createTask(
            id = 2,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )
        val child2 = TestTaskFactory.createTask(
            id = 3,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
        )
        repository.seed(root, child1, child2)

        val map = useCase.getStreakMap()
        assertEquals(1, map.size)
        assertEquals(2, map[1L])
    }

    @Test
    fun `getStreakMap returns correct counts for multiple chains`() = runTest {
        // Chain A: streak of 3
        val rootA = TestTaskFactory.createTask(id = 1, recurrenceType = 1)
        val a1 = TestTaskFactory.createTask(
            id = 10,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )
        val a2 = TestTaskFactory.createTask(
            id = 11,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
        )
        val a3 = TestTaskFactory.createTask(
            id = 12,
            parentRecurringTaskId = 1,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP + 2 * ONE_DAY_MS,
            dueAt = BASE_TIMESTAMP + 3 * ONE_DAY_MS,
        )

        // Chain B: streak of 1 (below threshold)
        val rootB = TestTaskFactory.createTask(id = 2, recurrenceType = 1)
        val b1 = TestTaskFactory.createTask(
            id = 20,
            parentRecurringTaskId = 2,
            recurrenceType = 1,
            isCompleted = true,
            completedAt = BASE_TIMESTAMP,
            dueAt = BASE_TIMESTAMP + ONE_DAY_MS,
        )

        repository.seed(rootA, a1, a2, a3, rootB, b1)

        val map = useCase.getStreakMap()
        assertEquals(1, map.size)
        assertEquals(3, map[1L])
        assertNull(map[2L])
    }
}
