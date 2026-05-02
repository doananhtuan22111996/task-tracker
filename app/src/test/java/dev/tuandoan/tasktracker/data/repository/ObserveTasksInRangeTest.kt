package dev.tuandoan.tasktracker.data.repository

import app.cash.turbine.test
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test for [dev.tuandoan.tasktracker.domain.repository.ITaskRepository.observeTasksInRange]
 * exercised via [FakeTaskRepository]. The Room-backed variant is covered separately by migration
 * instrumentation; this suite fixes the semantics the calendar use case will rely on.
 */
class ObserveTasksInRangeTest {

    private val day0 = TestTaskFactory.BASE_TIMESTAMP // 2024-01-15 12:00 UTC
    private val day1 = day0 + TestTaskFactory.ONE_DAY_MS
    private val day2 = day0 + 2 * TestTaskFactory.ONE_DAY_MS

    @Test
    fun `emits only tasks with dueAt inside the half-open window`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Before", dueAt = day0 - 1L),
            TestTaskFactory.createTask(id = 2L, title = "On start", dueAt = day0),
            TestTaskFactory.createTask(id = 3L, title = "In range", dueAt = day0 + TestTaskFactory.ONE_HOUR_MS),
            TestTaskFactory.createTask(id = 4L, title = "On end (excluded)", dueAt = day2),
            TestTaskFactory.createTask(id = 5L, title = "After", dueAt = day2 + 1L),
        )

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertEquals(listOf(2L, 3L), emitted.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `excludes archived tasks even when inside the window`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Active in range", dueAt = day0),
            TestTaskFactory.createTask(id = 2L, title = "Archived in range", dueAt = day1, isArchived = true),
        )

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertEquals(listOf(1L), emitted.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `includes completed tasks (calendar renders them as dimmed dots)`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Done", dueAt = day0, isCompleted = true, completedAt = day0),
            TestTaskFactory.createTask(id = 2L, title = "Open", dueAt = day1),
        )

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertEquals(2, emitted.size)
            assertTrue(emitted.any { it.id == 1L && it.isCompleted })
            assertTrue(emitted.any { it.id == 2L && !it.isCompleted })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `excludes tasks with null dueAt`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Undated", dueAt = null),
            TestTaskFactory.createTask(id = 2L, title = "Dated", dueAt = day0),
        )

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertEquals(listOf(2L), emitted.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits ordered by dueAt ascending`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, title = "Late", dueAt = day1),
            TestTaskFactory.createTask(id = 2L, title = "Early", dueAt = day0),
            TestTaskFactory.createTask(id = 3L, title = "Mid", dueAt = day0 + TestTaskFactory.ONE_HOUR_MS),
        )

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertEquals(listOf(2L, 3L, 1L), emitted.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty range yields empty list, never null`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, title = "Outside", dueAt = day2 + 1L))

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertTrue(emitted.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reactive emission when a new in-range task is inserted`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, title = "Initial", dueAt = day0))

        repo.observeTasksInRange(day0, day2).test {
            val first = awaitItem()
            assertEquals(listOf(1L), first.map { it.id })

            repo.insertTask(TestTaskFactory.createTask(id = 0L, title = "Added", dueAt = day1))

            val second = awaitItem()
            assertEquals(2, second.size)
            assertTrue(second.any { it.title == "Added" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not emit tasks at exactly the end boundary`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, title = "On end", dueAt = day2))

        repo.observeTasksInRange(day0, day2).test {
            val emitted = awaitItem()
            assertTrue(emitted.isEmpty())
            assertFalse(emitted.any { it.id == 1L })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
