package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.data.preferences.SettingsRepository
import dev.tuandoan.tasktracker.data.preferences.UserPreferences
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.ui.events.UiEvent
import dev.tuandoan.tasktracker.ui.manager.TaskBulkActionManager
import dev.tuandoan.tasktracker.ui.manager.TaskCrudManager
import dev.tuandoan.tasktracker.ui.manager.TaskOperationResult
import dev.tuandoan.tasktracker.ui.state.SelectionState
import dev.tuandoan.tasktracker.ui.state.TaskListState
import dev.tuandoan.tasktracker.ui.state.TaskListStateManager
import dev.tuandoan.tasktracker.ui.state.TaskSelectionStateManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RatingEligibilityTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var listStateManager: TaskListStateManager
    private lateinit var crudManager: TaskCrudManager
    private lateinit var selectionStateManager: TaskSelectionStateManager
    private lateinit var bulkActionManager: TaskBulkActionManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var taskManager: ITaskManager
    private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>

    private fun createViewModel(): TaskViewModel = TaskViewModel(
        context = context,
        listStateManager = listStateManager,
        crudManager = crudManager,
        selectionStateManager = selectionStateManager,
        bulkActionManager = bulkActionManager,
        settingsRepository = settingsRepository,
        taskManager = taskManager,
    )

    private fun mockCrudManagerToInvokeOnSuccess() {
        val scopeSlot = slot<CoroutineScope>()
        val operationSlot = slot<suspend () -> TaskOperationResult>()
        val onSuccessSlot = slot<(String) -> Unit>()

        every {
            crudManager.executeOperation(
                capture(scopeSlot),
                capture(operationSlot),
                capture(onSuccessSlot),
                any(),
            )
        } answers {
            onSuccessSlot.captured.invoke("success")
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        preferencesFlow = MutableStateFlow(UserPreferences())

        settingsRepository = mockk(relaxed = true) {
            every { userPreferences } returns preferencesFlow
        }

        taskManager = mockk(relaxed = true)

        val emptyTasksFlow = MutableStateFlow<List<Task>>(emptyList())
        val taskListState = TaskListState(
            allTasks = emptyTasksFlow,
            visibleTasks = emptyTasksFlow,
            searchQuery = MutableStateFlow(""),
            filter = MutableStateFlow(TaskFilter.ALL),
            tagFilter = MutableStateFlow(null),
            hasActiveSearch = MutableStateFlow(false),
            hasActiveFilter = MutableStateFlow(false),
            hasActiveTagFilter = MutableStateFlow(false),
            isLoading = MutableStateFlow(false),
        )
        listStateManager = mockk(relaxed = true) {
            every { initializeStateFlows(any()) } returns taskListState
        }

        val selectionState = SelectionState(
            selectedIds = MutableStateFlow(emptySet()),
            isSelectionMode = MutableStateFlow(false),
            selectedCount = MutableStateFlow(0),
        )
        selectionStateManager = mockk(relaxed = true) {
            every { initializeStateFlows(any()) } returns selectionState
        }

        crudManager = mockk(relaxed = true) {
            every { getArchivedTasks() } returns flowOf(emptyList())
            every { initializeErrorState(any()) } returns MutableStateFlow(null)
        }

        bulkActionManager = mockk(relaxed = true) {
            every { pendingBulkDeleteTasks } returns MutableStateFlow(emptyList())
            every { pendingBulkArchiveTasks } returns MutableStateFlow(emptyList())
            every { uiEvent } returns MutableSharedFlow()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleTaskCompletion emits ShowRatingPrompt when all conditions met`() = runTest {
        val fourDaysAgo = System.currentTimeMillis() - (4 * 86_400_000L)
        preferencesFlow.value = UserPreferences(
            firstLaunchDate = fourDaysAgo,
            ratingPromptShown = false,
        )
        every { taskManager.observeCompletedCount() } returns flowOf(5)
        mockCrudManagerToInvokeOnSuccess()

        val activeTask = Task(id = 1, title = "Test", isCompleted = false)
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.toggleTaskCompletion(activeTask)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Expected ShowRatingPrompt but got $event", event is UiEvent.ShowRatingPrompt)
        }
    }

    @Test
    fun `toggleTaskCompletion does not emit when ratingPromptShown is true`() = runTest {
        val fourDaysAgo = System.currentTimeMillis() - (4 * 86_400_000L)
        preferencesFlow.value = UserPreferences(
            firstLaunchDate = fourDaysAgo,
            ratingPromptShown = true,
        )
        every { taskManager.observeCompletedCount() } returns flowOf(5)
        mockCrudManagerToInvokeOnSuccess()

        val activeTask = Task(id = 1, title = "Test", isCompleted = false)
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.toggleTaskCompletion(activeTask)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `toggleTaskCompletion does not emit when completedCount below threshold`() = runTest {
        val fourDaysAgo = System.currentTimeMillis() - (4 * 86_400_000L)
        preferencesFlow.value = UserPreferences(
            firstLaunchDate = fourDaysAgo,
            ratingPromptShown = false,
        )
        every { taskManager.observeCompletedCount() } returns flowOf(2)
        mockCrudManagerToInvokeOnSuccess()

        val activeTask = Task(id = 1, title = "Test", isCompleted = false)
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.toggleTaskCompletion(activeTask)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `toggleTaskCompletion does not emit when days since launch below threshold`() = runTest {
        val oneDayAgo = System.currentTimeMillis() - (1 * 86_400_000L)
        preferencesFlow.value = UserPreferences(
            firstLaunchDate = oneDayAgo,
            ratingPromptShown = false,
        )
        every { taskManager.observeCompletedCount() } returns flowOf(5)
        mockCrudManagerToInvokeOnSuccess()

        val activeTask = Task(id = 1, title = "Test", isCompleted = false)
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.toggleTaskCompletion(activeTask)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `toggleTaskCompletion does not emit when firstLaunchDate is zero`() = runTest {
        preferencesFlow.value = UserPreferences(
            firstLaunchDate = 0L,
            ratingPromptShown = false,
        )
        every { taskManager.observeCompletedCount() } returns flowOf(5)
        mockCrudManagerToInvokeOnSuccess()

        val activeTask = Task(id = 1, title = "Test", isCompleted = false)
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.toggleTaskCompletion(activeTask)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `ensureFirstLaunchDate is called on ViewModel init`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coVerify { settingsRepository.ensureFirstLaunchDate() }
    }
}
