package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.ReminderOption
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class TaskEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var fakeTaskManager: FakeEditorTaskManager
    private lateinit var formUseCase: TaskFormUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true) {
            every { getString(any()) } returns "test string"
            every { getString(any(), *anyVararg()) } returns "test string"
        }
        fakeTaskManager = FakeEditorTaskManager()
        formUseCase = TaskFormUseCase(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(taskId: Long? = null): TaskEditorViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (taskId != null) {
                set("taskId", taskId)
            }
        }
        return TaskEditorViewModel(context, fakeTaskManager, formUseCase, savedStateHandle)
    }

    // === Create Mode Tests ===

    @Test
    fun `create mode - isEditMode is false when no taskId`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.isEditMode)
    }

    @Test
    fun `create mode - initial form fields are empty`() {
        val viewModel = createViewModel()
        assertEquals("", viewModel.taskTitle.value)
        assertEquals("", viewModel.taskDescription.value)
        assertNull(viewModel.dueAt.value)
        assertEquals(ReminderOption.NONE, viewModel.reminderOption.value)
        assertEquals("", viewModel.tag.value)
        assertEquals(1, viewModel.priority.value) // MEDIUM default
        assertFalse(viewModel.isPinned.value)
    }

    @Test
    fun `updateTitle updates taskTitle state`() {
        val viewModel = createViewModel()
        viewModel.updateTitle("New Task")
        assertEquals("New Task", viewModel.taskTitle.value)
    }

    @Test
    fun `updateTitle rejects input exceeding max length`() {
        val viewModel = createViewModel()
        val longTitle = "A".repeat(TaskFormUseCase.MAX_TITLE_LENGTH + 1)
        viewModel.updateTitle(longTitle)
        assertEquals("", viewModel.taskTitle.value) // should remain unchanged
    }

    @Test
    fun `updateDescription updates taskDescription state`() {
        val viewModel = createViewModel()
        viewModel.updateDescription("Some description")
        assertEquals("Some description", viewModel.taskDescription.value)
    }

    @Test
    fun `updatePriority updates priority within valid range`() {
        val viewModel = createViewModel()
        viewModel.updatePriority(0) // LOW
        assertEquals(0, viewModel.priority.value)

        viewModel.updatePriority(2) // HIGH
        assertEquals(2, viewModel.priority.value)
    }

    @Test
    fun `updatePriority ignores invalid values`() {
        val viewModel = createViewModel()
        viewModel.updatePriority(5) // Invalid
        assertEquals(1, viewModel.priority.value) // stays at MEDIUM default

        viewModel.updatePriority(-1)
        assertEquals(1, viewModel.priority.value)
    }

    @Test
    fun `updateTag updates tag state`() {
        val viewModel = createViewModel()
        viewModel.updateTag("work")
        assertEquals("work", viewModel.tag.value)
    }

    @Test
    fun `updateTag rejects input exceeding max length`() {
        val viewModel = createViewModel()
        val longTag = "A".repeat(TaskFormUseCase.MAX_TAG_LENGTH + 1)
        viewModel.updateTag(longTag)
        assertEquals("", viewModel.tag.value)
    }

    @Test
    fun `updateIsPinned updates isPinned state`() {
        val viewModel = createViewModel()
        viewModel.updateIsPinned(true)
        assertTrue(viewModel.isPinned.value)
    }

    @Test
    fun `hasChanges is false initially in create mode`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.hasChanges.value)
    }

    @Test
    fun `hasChanges is true after setting title in create mode`() {
        val viewModel = createViewModel()
        viewModel.updateTitle("New Task")
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `clearDueDate clears dueAt and reminder`() {
        val viewModel = createViewModel()
        viewModel.updateDueAt(System.currentTimeMillis() + 86400000L)
        viewModel.updateReminderOption(ReminderOption.HOURS_1)
        viewModel.clearDueDate()

        assertNull(viewModel.dueAt.value)
        assertEquals(ReminderOption.NONE, viewModel.reminderOption.value)
    }

    // === Edit Mode Tests ===

    @Test
    fun `edit mode - isEditMode is true when taskId provided`() {
        val task = TestTaskFactory.createTask(id = 1, title = "Existing Task")
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        assertTrue(viewModel.isEditMode)
    }

    @Test
    fun `edit mode - loads task data into form fields`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Existing Task",
            description = "Some description",
            tag = "work",
            priority = 2,
            isPinned = true,
        )
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        // Wait for loading to complete
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Existing Task", viewModel.taskTitle.value)
        assertEquals("Some description", viewModel.taskDescription.value)
        assertEquals("work", viewModel.tag.value)
        assertEquals(2, viewModel.priority.value)
        assertTrue(viewModel.isPinned.value)
    }

    @Test
    fun `edit mode - hasChanges is false when no edits made`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Existing Task")
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.hasChanges.value)
    }

    @Test
    fun `edit mode - hasChanges is true after changing title`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Existing Task")
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTitle("Modified Task")
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `edit mode - isEditMode is true when taskId provided even if task not found`() = runTest {
        fakeTaskManager.taskToReturn = null

        val viewModel = createViewModel(taskId = 999L)
        testDispatcher.scheduler.advanceUntilIdle()

        // When task not found, isEditMode is still true (based on taskId presence)
        assertTrue(viewModel.isEditMode)
        // But form stays empty since no task was loaded
        assertEquals("", viewModel.taskTitle.value)
    }

    // === Save Tests ===

    @Test
    fun `saveTask creates new task in create mode`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("New Task")
        viewModel.updateDescription("Description")
        viewModel.updateTag("work")

        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeTaskManager.createCalled)
        assertEquals("New Task", fakeTaskManager.lastCreatedTitle)
    }

    @Test
    fun `saveTask updates existing task in edit mode`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Old Title")
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTitle("New Title")
        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeTaskManager.updateCalled)
        assertEquals("New Title", fakeTaskManager.lastUpdatedTask?.title)
    }

    @Test
    fun `saveTask emits TaskSaved event on success`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("New Task")

        viewModel.events.test {
            viewModel.saveTask()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is TaskEditorEvent.TaskSaved)
        }
    }

    @Test
    fun `clearError clears error message`() {
        val viewModel = createViewModel()
        // Error message starts as null
        assertNull(viewModel.errorMessage.value)
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }

    // === Validation Tests ===

    @Test
    fun `isSaveEnabled is false when title is empty`() = runTest {
        val viewModel = createViewModel()
        val enabled = viewModel.isSaveEnabled.first()
        assertFalse(enabled)
    }

    @Test
    fun `isSaveEnabled is true when title is valid and has changes`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Valid Title")
        val enabled = viewModel.isSaveEnabled.first()
        assertTrue(enabled)
    }
}

private class FakeEditorTaskManager : ITaskManager {

    var taskToReturn: Task? = null
    var createCalled = false
    var updateCalled = false
    var lastCreatedTitle: String? = null
    var lastUpdatedTask: Task? = null

    override suspend fun getTaskById(id: Long): Task? = taskToReturn
    override fun getAllTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())

    override suspend fun createTask(title: String, description: String): Long {
        createCalled = true
        lastCreatedTitle = title
        return 1L
    }

    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Long {
        createCalled = true
        lastCreatedTitle = title
        return 1L
    }

    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Long {
        createCalled = true
        lastCreatedTitle = title
        return 1L
    }

    override suspend fun updateTask(task: Task) {
        updateCalled = true
        lastUpdatedTask = task
    }

    override suspend fun updateTaskContent(taskId: Long, title: String, description: String) {}
    override suspend fun updateTaskContent(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
    ): Boolean = true

    override suspend fun updateTaskContent(
        taskId: Long,
        title: String,
        description: String,
        dueAt: Long?,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Boolean = true

    override suspend fun deleteTask(task: Task) {}
    override suspend fun restoreTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTaskCompletion(task: Task) {}
    override suspend fun markTaskComplete(task: Task) {}
    override suspend fun markTaskIncomplete(task: Task) {}
    override suspend fun setCompletedBulk(ids: List<Long>, completed: Boolean) {}
    override suspend fun deleteTasksByIds(ids: List<Long>) {}
    override suspend fun restoreTasks(tasks: List<Task>): Result<Unit> = Result.success(Unit)
    override fun getActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun setPinned(taskId: Long, pinned: Boolean) {}
    override suspend fun setPriority(taskId: Long, priority: Int) {}
    override fun getArchivedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun archiveTask(taskId: Long) {}
    override suspend fun unarchiveTask(taskId: Long) {}
    override suspend fun archiveTasks(ids: List<Long>) {}
    override suspend fun unarchiveTasks(ids: List<Long>) {}
    override suspend fun hardDeleteTask(taskId: Long) {}
    override suspend fun hardDeleteTasks(ids: List<Long>) {}
    override fun observeActiveCount(): Flow<Int> = MutableStateFlow(0)
    override fun observeCompletedCount(): Flow<Int> = MutableStateFlow(0)
    override fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        MutableStateFlow(0)
    override fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> = MutableStateFlow(0)
    override fun observeOverdueCount(nowMillis: Long): Flow<Int> = MutableStateFlow(0)
    override fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>> =
        MutableStateFlow(emptyList())
}
