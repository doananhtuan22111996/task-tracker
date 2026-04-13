package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.model.TaskSort
import dev.tuandoan.tasktracker.domain.service.TaskSortService
import dev.tuandoan.tasktracker.domain.usecase.StreakUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFilterUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskSearchUseCase
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.ui.manager.TaskBulkActionManager
import dev.tuandoan.tasktracker.ui.manager.TaskCrudManager
import dev.tuandoan.tasktracker.ui.state.TaskFormStateManager
import dev.tuandoan.tasktracker.ui.state.TaskListStateManager
import dev.tuandoan.tasktracker.ui.state.TaskSelectionStateManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var repository: FakeTaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var selectionStateManager: TaskSelectionStateManager
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true) {
            every { getString(any()) } returns "test string"
            every { getString(any(), *anyVararg()) } returns "test string"
        }
        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        selectionStateManager = TaskSelectionStateManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TaskViewModel {
        val taskManager = TaskManager(repository, scheduler, FakeWidgetUpdater())
        val crudUseCase = TaskCrudUseCase(taskManager, context)
        val searchUseCase = TaskSearchUseCase()
        val filterUseCase = TaskFilterUseCase()
        val sortService = TaskSortService()
        val formUseCase = TaskFormUseCase(context)
        val formStateManager = TaskFormStateManager(formUseCase, context)
        val listStateManager = TaskListStateManager(crudUseCase, searchUseCase, filterUseCase, sortService)
        val crudManager = TaskCrudManager(crudUseCase, formStateManager, context)
        val bulkActionManager = TaskBulkActionManager(context, crudManager, selectionStateManager)

        val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
            every { userPreferences } returns flowOf(UserPreferences())
            coEvery { getSortPreference() } returns TaskSort()
        }

        return TaskViewModel(
            context,
            listStateManager,
            crudManager,
            selectionStateManager,
            bulkActionManager,
            settingsRepository,
            taskManager,
            StreakUseCase(repository),
            TaskSortService(),
        )
    }

    // === Initial State Tests ===

    @Test
    fun `initial state has empty task lists`() = runTest {
        viewModel = createViewModel()
        assertEquals(emptyList<Any>(), viewModel.allTasks.value)
    }

    @Test
    fun `initial state has no search query`() {
        viewModel = createViewModel()
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `initial state filter is ALL`() {
        viewModel = createViewModel()
        assertEquals(TaskFilter.ALL, viewModel.filter.value)
    }

    @Test
    fun `initial state has no tag filter`() {
        viewModel = createViewModel()
        assertNull(viewModel.tagFilter.value)
    }

    @Test
    fun `initial state has no error`() {
        viewModel = createViewModel()
        assertNull(viewModel.errorMessage.value)
    }

    // === Task List Observation Tests ===

    @Test
    fun `allTasks reflects seeded tasks`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        viewModel = createViewModel()

        viewModel.allTasks.test {
            val result = awaitItem()
            if (result.isEmpty()) {
                // Initial empty value, wait for actual emission
                assertEquals(3, awaitItem().size)
            } else {
                assertEquals(3, result.size)
            }
        }
    }

    // === Tag Filter Tests ===

    @Test
    fun `availableTags derived from all tasks`() = runTest {
        val t1 = TestTaskFactory.createTask(id = 1, tag = "work")
        val t2 = TestTaskFactory.createTask(id = 2, tag = "personal")
        val t3 = TestTaskFactory.createTask(id = 3, tag = "work") // duplicate tag
        val t4 = TestTaskFactory.createTask(id = 4) // no tag
        repository.seed(t1, t2, t3, t4)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.availableTags.test {
            val tags = awaitItem()
            assertEquals(listOf("personal", "work"), tags)
        }
    }

    @Test
    fun `setTagFilter updates tag filter state`() {
        viewModel = createViewModel()
        viewModel.setTagFilter("work")

        assertEquals("work", viewModel.tagFilter.value)
    }

    @Test
    fun `clearTagFilter resets tag filter to null`() {
        viewModel = createViewModel()
        viewModel.setTagFilter("work")
        viewModel.clearTagFilter()

        assertNull(viewModel.tagFilter.value)
    }

    // === Selection Tests (using selectionStateManager directly) ===

    @Test
    fun `enterSelection updates selected ids`() {
        viewModel = createViewModel()
        viewModel.enterSelection(1L)

        assertTrue(selectionStateManager.selectedIds.value.contains(1L))
    }

    @Test
    fun `toggleSelection adds and removes ids`() {
        viewModel = createViewModel()
        viewModel.enterSelection(1L)
        viewModel.toggleSelection(2L)

        assertEquals(2, selectionStateManager.selectedIds.value.size)

        viewModel.toggleSelection(1L)
        assertEquals(1, selectionStateManager.selectedIds.value.size)
        assertFalse(selectionStateManager.selectedIds.value.contains(1L))
    }

    @Test
    fun `clearSelection empties selected ids`() {
        viewModel = createViewModel()
        viewModel.enterSelection(1L)
        viewModel.clearSelection()

        assertTrue(selectionStateManager.selectedIds.value.isEmpty())
    }

    @Test
    fun `selectAll sets all provided ids`() {
        viewModel = createViewModel()
        viewModel.selectAll(listOf(1L, 2L, 3L))

        assertEquals(3, selectionStateManager.selectedIds.value.size)
    }

    // === Search Tests ===

    @Test
    fun `updateSearchQuery updates search state`() {
        viewModel = createViewModel()
        viewModel.updateSearchQuery("test")

        assertEquals("test", viewModel.searchQuery.value)
    }

    @Test
    fun `clearSearch resets query`() {
        viewModel = createViewModel()
        viewModel.updateSearchQuery("test")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchQuery.value)
    }

    // === Filter Tests ===

    @Test
    fun `setFilter updates current filter`() {
        viewModel = createViewModel()
        viewModel.setFilter(TaskFilter.COMPLETED)

        assertEquals(TaskFilter.COMPLETED, viewModel.filter.value)
    }

    @Test
    fun `setFilter to ACTIVE updates filter state`() {
        viewModel = createViewModel()
        viewModel.setFilter(TaskFilter.ACTIVE)

        assertEquals(TaskFilter.ACTIVE, viewModel.filter.value)
    }

    // === Delete Confirmation Flow ===

    @Test
    fun `deleteTask sets pendingDeleteTask`() {
        viewModel = createViewModel()
        val task = TestTaskFactory.createTask(id = 1, title = "Test")
        viewModel.deleteTask(task)

        assertEquals(task, viewModel.pendingDeleteTask.value)
    }

    @Test
    fun `cancelDeleteTask clears pendingDeleteTask`() {
        viewModel = createViewModel()
        val task = TestTaskFactory.createTask(id = 1, title = "Test")
        viewModel.deleteTask(task)
        viewModel.cancelDeleteTask()

        assertNull(viewModel.pendingDeleteTask.value)
    }

    @Test
    fun `archiveTask sets pendingDeleteTask for archive confirmation`() {
        viewModel = createViewModel()
        val task = TestTaskFactory.createTask(id = 1, title = "Test")
        viewModel.archiveTask(task)

        assertEquals(task, viewModel.pendingDeleteTask.value)
    }

    // === Archived Tag Filter Tests ===

    @Test
    fun `setArchivedTagFilter updates archived tag filter`() {
        viewModel = createViewModel()
        viewModel.setArchivedTagFilter("work")

        assertEquals("work", viewModel.archivedTagFilter.value)
    }

    @Test
    fun `setArchivedTagFilter null clears filter`() {
        viewModel = createViewModel()
        viewModel.setArchivedTagFilter("work")
        viewModel.setArchivedTagFilter(null)

        assertNull(viewModel.archivedTagFilter.value)
    }

    // === Error Management ===

    @Test
    fun `clearError does not throw`() {
        viewModel = createViewModel()
        viewModel.clearError() // should not throw
        assertNull(viewModel.errorMessage.value)
    }
}
