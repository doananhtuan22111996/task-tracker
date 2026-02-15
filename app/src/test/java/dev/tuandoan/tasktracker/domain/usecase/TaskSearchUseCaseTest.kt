package dev.tuandoan.tasktracker.domain.usecase

import app.cash.turbine.test
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskSearchUseCaseTest {

    private lateinit var useCase: TaskSearchUseCase

    @Before
    fun setup() {
        useCase = TaskSearchUseCase()
    }

    // === filterTasksBySearch ===

    @Test
    fun `filterTasksBySearch with empty query returns all tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        val result = useCase.filterTasksBySearch(tasks, "")
        assertEquals(3, result.size)
    }

    @Test
    fun `filterTasksBySearch with whitespace-only query returns all tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        val result = useCase.filterTasksBySearch(tasks, "   ")
        assertEquals(3, result.size)
    }

    @Test
    fun `filterTasksBySearch matches title case-insensitively`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Buy GROCERIES"),
            TestTaskFactory.createTask(id = 2, title = "Read book"),
            TestTaskFactory.createTask(id = 3, title = "Get groceries from store"),
        )

        val result = useCase.filterTasksBySearch(tasks, "groceries")

        assertEquals(2, result.size)
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `filterTasksBySearch matches description`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Task 1", description = "Buy milk from store"),
            TestTaskFactory.createTask(id = 2, title = "Task 2", description = "Read a book"),
        )

        val result = useCase.filterTasksBySearch(tasks, "milk")

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `filterTasksBySearch trims search query`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Find me"),
        )

        val result = useCase.filterTasksBySearch(tasks, "  Find  ")

        assertEquals(1, result.size)
    }

    @Test
    fun `filterTasksBySearch returns empty for no matches`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Buy milk"),
        )

        val result = useCase.filterTasksBySearch(tasks, "xyz")

        assertTrue(result.isEmpty())
    }

    // === searchQuery state ===

    @Test
    fun `updateSearchQuery updates searchQuery state`() = runTest {
        useCase.searchQuery.test {
            assertEquals("", awaitItem()) // initial

            useCase.updateSearchQuery("hello")
            assertEquals("hello", awaitItem())
        }
    }

    @Test
    fun `clearSearch resets query to empty`() = runTest {
        useCase.updateSearchQuery("test")

        useCase.clearSearch()

        useCase.searchQuery.test {
            assertEquals("", awaitItem())
        }
    }

    // === hasActiveSearch ===

    @Test
    fun `hasActiveSearch is false for empty query`() = runTest {
        useCase.hasActiveSearch().test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `hasActiveSearch is true when query is non-blank`() = runTest {
        useCase.updateSearchQuery("test")

        useCase.hasActiveSearch().test {
            assertTrue(awaitItem())
        }
    }
}
