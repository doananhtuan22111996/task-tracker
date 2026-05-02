package dev.tuandoan.tasktracker.data.repository

import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Contract test for
 * [dev.tuandoan.tasktracker.domain.repository.ITaskRepository.findChainTaskOnDate]
 * exercised via [FakeTaskRepository]. The Room-backed variant is covered by migration
 * instrumentation; this suite locks the semantics the CAL-23 materialize path depends on:
 * half-open `[startMillis, endMillis)` window, chain scope (id == rootId OR parent == rootId),
 * and archived exclusion.
 */
class FindChainTaskOnDateTest {

    private val day0 = TestTaskFactory.BASE_TIMESTAMP // 2024-01-15 12:00 UTC
    private val dayStart = day0
    private val dayEnd = day0 + TestTaskFactory.ONE_DAY_MS

    @Test
    fun `returns row whose id matches rootId and dueAt falls inside window`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = day0 + TestTaskFactory.ONE_HOUR_MS))

        val found = repo.findChainTaskOnDate(rootId = 1L, startMillis = dayStart, endMillis = dayEnd)

        assertEquals(1L, found?.id)
    }

    @Test
    fun `returns child row whose parentRecurringTaskId matches rootId`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = day0 - TestTaskFactory.ONE_DAY_MS),
            TestTaskFactory.createTask(
                id = 42L,
                dueAt = day0 + TestTaskFactory.ONE_HOUR_MS,
                parentRecurringTaskId = 1L,
            ),
        )

        val found = repo.findChainTaskOnDate(rootId = 1L, startMillis = dayStart, endMillis = dayEnd)

        assertEquals(42L, found?.id)
    }

    @Test
    fun `returns null when no chain row falls inside the window`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 1L, dueAt = day0 - TestTaskFactory.ONE_DAY_MS),
            TestTaskFactory.createTask(
                id = 2L,
                dueAt = day0 + 2 * TestTaskFactory.ONE_DAY_MS,
                parentRecurringTaskId = 1L,
            ),
        )

        val found = repo.findChainTaskOnDate(rootId = 1L, startMillis = dayStart, endMillis = dayEnd)

        assertNull(found)
    }

    @Test
    fun `excludes archived rows even when they match the window`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(
                id = 1L,
                dueAt = day0 + TestTaskFactory.ONE_HOUR_MS,
                isArchived = true,
            ),
        )

        val found = repo.findChainTaskOnDate(rootId = 1L, startMillis = dayStart, endMillis = dayEnd)

        assertNull(found)
    }

    @Test
    fun `end boundary is exclusive`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(TestTaskFactory.createTask(id = 1L, dueAt = dayEnd))

        val found = repo.findChainTaskOnDate(rootId = 1L, startMillis = dayStart, endMillis = dayEnd)

        assertNull(found)
    }

    @Test
    fun `does not return rows from a different chain`() = runTest {
        val repo = FakeTaskRepository()
        repo.seed(
            TestTaskFactory.createTask(id = 7L, dueAt = day0 + TestTaskFactory.ONE_HOUR_MS),
            TestTaskFactory.createTask(
                id = 8L,
                dueAt = day0 + 2 * TestTaskFactory.ONE_HOUR_MS,
                parentRecurringTaskId = 9L,
            ),
        )

        val found = repo.findChainTaskOnDate(rootId = 1L, startMillis = dayStart, endMillis = dayEnd)

        assertNull(found)
    }
}
