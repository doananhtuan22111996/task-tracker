package dev.tuandoan.tasktracker.ui.state

import android.content.Context
import app.cash.turbine.test
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.model.CompletedGrouping
import dev.tuandoan.tasktracker.domain.model.SortDirection
import dev.tuandoan.tasktracker.domain.model.SortKey
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.domain.service.TaskSortService
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFilterUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskSearchUseCase
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.ui.viewmodel.TaskFilter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListStateManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: FakeTaskRepository
    private lateinit var manager: TaskListStateManager
    private lateinit var searchUseCase: TaskSearchUseCase
    private lateinit var filterUseCase: TaskFilterUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = FakeTaskRepository()
        val context: Context = mockk(relaxed = true) {
            every { getString(any()) } returns "test"
            every { getString(any(), *anyVararg()) } returns "test"
        }
        val taskManager = TaskManager(repository, FakeReminderScheduler(), FakeWidgetUpdater())
        val crudUseCase = TaskCrudUseCase(taskManager, context)
        searchUseCase = TaskSearchUseCase()
        filterUseCase = TaskFilterUseCase()
        val sortService = TaskSortService()

        manager = TaskListStateManager(crudUseCase, searchUseCase, filterUseCase, sortService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === Default State ===

    @Test
    fun `default sort is DUE_DATE ASC COMPLETED_LAST`() {
        val sort = manager.currentSort.value
        assertEquals(SortKey.DUE_DATE, sort.key)
        assertEquals(SortDirection.ASC, sort.direction)
        assertEquals(CompletedGrouping.COMPLETED_LAST, sort.completedGrouping)
    }

    @Test
    fun `default tag filter is null`() {
        assertEquals(null, manager.tagFilter.value)
    }

    // === Visible Tasks Pipeline ===

    @Test
    fun `visibleTasks emits all tasks when no filters active`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                assertEquals(3, awaitItem().size)
            } else {
                assertEquals(3, result.size)
            }
        }
    }

    @Test
    fun `visibleTasks respects status filter ACTIVE`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Done"),
        )

        filterUseCase.setFilter(TaskFilter.ACTIVE)
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                val actual = awaitItem()
                assertEquals(1, actual.size)
                assertEquals("Active", actual[0].title)
            } else {
                assertEquals(1, result.size)
                assertEquals("Active", result[0].title)
            }
        }
    }

    @Test
    fun `visibleTasks respects status filter COMPLETED`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.completedTask(id = 2, title = "Done"),
        )

        filterUseCase.setFilter(TaskFilter.COMPLETED)
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                val actual = awaitItem()
                assertEquals(1, actual.size)
                assertEquals("Done", actual[0].title)
            } else {
                assertEquals(1, result.size)
                assertEquals("Done", result[0].title)
            }
        }
    }

    @Test
    fun `visibleTasks respects tag filter`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Work", tag = "work"),
            TestTaskFactory.createTask(id = 2, title = "Personal", tag = "personal"),
            TestTaskFactory.createTask(id = 3, title = "NoTag"),
        )

        manager.setTagFilter("work")
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                val actual = awaitItem()
                assertEquals(1, actual.size)
                assertEquals("Work", actual[0].title)
            } else {
                assertEquals(1, result.size)
                assertEquals("Work", result[0].title)
            }
        }
    }

    @Test
    fun `clearTagFilter shows all tasks again`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Work", tag = "work"),
            TestTaskFactory.createTask(id = 2, title = "Personal", tag = "personal"),
        )

        manager.setTagFilter("work")
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        // Verify filtered state
        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                assertEquals(1, awaitItem().size)
            } else {
                assertEquals(1, result.size)
            }
        }

        // Clear and verify all tasks visible
        manager.clearTagFilter()
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.size < 2) {
                assertEquals(2, awaitItem().size)
            } else {
                assertEquals(2, result.size)
            }
        }
    }

    // === Sort Operations ===

    @Test
    fun `updateSort changes current sort`() {
        val newSort = TaskSort(SortKey.TITLE, SortDirection.ASC)
        manager.updateSort(newSort)

        assertEquals(SortKey.TITLE, manager.currentSort.value.key)
        assertEquals(SortDirection.ASC, manager.currentSort.value.direction)
    }

    @Test
    fun `initializeSort sets sort from saved preference`() {
        val saved = TaskSort(SortKey.PRIORITY, SortDirection.DESC, CompletedGrouping.COMPLETED_FIRST)
        manager.initializeSort(saved)

        assertEquals(SortKey.PRIORITY, manager.currentSort.value.key)
        assertEquals(SortDirection.DESC, manager.currentSort.value.direction)
        assertEquals(CompletedGrouping.COMPLETED_FIRST, manager.currentSort.value.completedGrouping)
    }

    @Test
    fun `visibleTasks sorted by title ASC`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1, title = "Cherry", createdAt = 1000L),
            TestTaskFactory.createTask(id = 2, title = "Apple", createdAt = 2000L),
            TestTaskFactory.createTask(id = 3, title = "Banana", createdAt = 3000L),
        )

        manager.updateSort(TaskSort(SortKey.TITLE, SortDirection.ASC))
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                val actual = awaitItem()
                assertEquals(listOf("Apple", "Banana", "Cherry"), actual.map { it.title })
            } else {
                assertEquals(listOf("Apple", "Banana", "Cherry"), result.map { it.title })
            }
        }
    }

    @Test
    fun `visibleTasks with COMPLETED_LAST groups active before completed`() = runTest {
        repository.seed(
            TestTaskFactory.completedTask(id = 1, title = "Done", createdAt = 3000L),
            TestTaskFactory.createTask(id = 2, title = "Active", createdAt = 1000L),
        )

        manager.updateSort(
            TaskSort(SortKey.CREATED_AT, SortDirection.DESC, CompletedGrouping.COMPLETED_LAST),
        )
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.visibleTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                val actual = awaitItem()
                assertFalse(actual[0].isCompleted)
                assertTrue(actual[1].isCompleted)
            } else {
                assertFalse(result[0].isCompleted)
                assertTrue(result[1].isCompleted)
            }
        }
    }

    // === State Indicator Flows ===

    @Test
    fun `hasActiveTagFilter is true when tag is set`() = runTest {
        manager.setTagFilter("work")
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.hasActiveTagFilter.test {
            val first = awaitItem()
            if (!first) {
                // Initial value was false, wait for the mapped emission
                assertTrue(awaitItem())
            } else {
                assertTrue(first)
            }
        }
    }

    @Test
    fun `hasActiveTagFilter is false when tag is cleared`() = runTest {
        val state = manager.initializeStateFlows(backgroundScope)
        advanceUntilIdle()

        state.hasActiveTagFilter.test {
            assertFalse(awaitItem())
        }
    }

    // === Search Operations ===

    @Test
    fun `updateSearchQuery delegates to search use case`() {
        manager.updateSearchQuery("test query")
        assertEquals("test query", searchUseCase.searchQuery.value)
    }

    @Test
    fun `clearSearch delegates to search use case`() {
        manager.updateSearchQuery("test query")
        manager.clearSearch()
        assertEquals("", searchUseCase.searchQuery.value)
    }

    // === Filter Operations ===

    @Test
    fun `setFilter delegates to filter use case`() {
        manager.setFilter(TaskFilter.COMPLETED)
        assertEquals(TaskFilter.COMPLETED, filterUseCase.filter.value)
    }
}
