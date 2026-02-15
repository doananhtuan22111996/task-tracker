package dev.tuandoan.tasktracker.domain.usecase

import app.cash.turbine.test
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskFilterUseCaseTest {

    private lateinit var useCase: TaskFilterUseCase

    @Before
    fun setup() {
        useCase = TaskFilterUseCase()
    }

    // === filterTasksByStatus ===

    @Test
    fun `filterTasksByStatus ALL returns all tasks`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
        )

        val result = useCase.filterTasksByStatus(tasks, TaskFilter.ALL)

        assertEquals(2, result.size)
    }

    @Test
    fun `filterTasksByStatus ACTIVE returns only active tasks`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Done"),
            TestTaskFactory.createTask(id = 3, title = "Also active"),
        )

        val result = useCase.filterTasksByStatus(tasks, TaskFilter.ACTIVE)

        assertEquals(2, result.size)
        assertTrue(result.all { !it.isCompleted })
    }

    @Test
    fun `filterTasksByStatus COMPLETED returns only completed tasks`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
            TestTaskFactory.completedTask(id = 3),
        )

        val result = useCase.filterTasksByStatus(tasks, TaskFilter.COMPLETED)

        assertEquals(2, result.size)
        assertTrue(result.all { it.isCompleted })
    }

    @Test
    fun `filterTasksByStatus with empty list returns empty`() {
        val result = useCase.filterTasksByStatus(emptyList(), TaskFilter.ALL)
        assertTrue(result.isEmpty())
    }

    // === filter state ===

    @Test
    fun `initial filter is ALL`() = runTest {
        useCase.filter.test {
            assertEquals(TaskFilter.ALL, awaitItem())
        }
    }

    @Test
    fun `setFilter updates filter state`() = runTest {
        useCase.filter.test {
            assertEquals(TaskFilter.ALL, awaitItem())

            useCase.setFilter(TaskFilter.ACTIVE)
            assertEquals(TaskFilter.ACTIVE, awaitItem())
        }
    }

    @Test
    fun `resetFilter sets filter back to ALL`() = runTest {
        useCase.setFilter(TaskFilter.COMPLETED)

        useCase.resetFilter()

        useCase.filter.test {
            assertEquals(TaskFilter.ALL, awaitItem())
        }
    }

    // === hasActiveFilter ===

    @Test
    fun `hasActiveFilter is false when filter is ALL`() = runTest {
        useCase.hasActiveFilter().test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `hasActiveFilter is true when filter is not ALL`() = runTest {
        useCase.setFilter(TaskFilter.COMPLETED)

        useCase.hasActiveFilter().test {
            assertTrue(awaitItem())
        }
    }
}
