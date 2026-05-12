package dev.tuandoan.tasktracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.tuandoan.tasktracker.data.database.DailyCount
import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.ITaskManager
import dev.tuandoan.tasktracker.domain.model.DueDatePreset
import dev.tuandoan.tasktracker.domain.model.ReminderOption
import dev.tuandoan.tasktracker.domain.usecase.SubtaskUseCase
import dev.tuandoan.tasktracker.domain.usecase.TagManagementUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.fakeAnalyticsLogger
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var fakeTaskManager: FakeEditorTaskManager
    private lateinit var formUseCase: TaskFormUseCase
    private lateinit var tagManagementUseCase: TagManagementUseCase
    private lateinit var subtaskRepository: FakeSubtaskRepository
    private lateinit var subtaskUseCase: SubtaskUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true) {
            every { getString(any()) } returns "test string"
            every { getString(any(), *anyVararg()) } returns "test string"
        }
        fakeTaskManager = FakeEditorTaskManager()
        formUseCase = TaskFormUseCase(context)
        tagManagementUseCase = mockk(relaxed = true)
        subtaskRepository = FakeSubtaskRepository()
        subtaskUseCase = SubtaskUseCase(subtaskRepository, fakeAnalyticsLogger())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(taskId: Long? = null, initialDueAt: Long? = null): TaskEditorViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (taskId != null) {
                set("taskId", taskId)
            }
            if (initialDueAt != null) {
                set("initialDueAt", initialDueAt)
            }
        }
        return TaskEditorViewModel(
            context,
            fakeTaskManager,
            formUseCase,
            tagManagementUseCase,
            subtaskUseCase,
            savedStateHandle,
        )
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

    // ── Create mode with prefilled dueAt (CAL-19) ──

    @Test
    fun `create mode - initialDueAt seeds dueAt without time-of-day`() {
        val targetMillis = 1_735_689_600_000L // 2025-01-01 00:00 UTC
        val viewModel = createViewModel(initialDueAt = targetMillis)

        assertEquals(targetMillis, viewModel.dueAt.value)
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `create mode - sentinel negative initialDueAt is ignored`() {
        // MainActivity uses -1L as "no value" since NavType.LongType is non-nullable.
        val viewModel = createViewModel(initialDueAt = -1L)
        assertNull(viewModel.dueAt.value)
    }

    @Test
    fun `edit mode - taskId wins over initialDueAt when both are present`() = runTest {
        // Defensive guard: MainActivity never passes both today, but the VM init's
        // `else if` contract must keep edit-mode loading as the single source of truth
        // if anyone ever wires both args on the same route.
        val taskDueAt = 1_700_000_000_000L
        val seedDueAt = 1_735_689_600_000L
        val task = TestTaskFactory.createTask(id = 1, title = "Existing", dueAt = taskDueAt)
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L, initialDueAt = seedDueAt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(taskDueAt, viewModel.dueAt.value)
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
    fun `saveTask normalizes tag and resolves color for lowercase user input`() = runTest {
        // User types "work" but canonical storage is "WORK". The color lookup must happen
        // against the canonical form, otherwise existing color inheritance breaks.
        io.mockk.coEvery { tagManagementUseCase.getTagColor("WORK") } returns "#FF0000"
        val task = TestTaskFactory.createTask(id = 1, title = "T")
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTag("work")
        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("WORK", fakeTaskManager.lastUpdatedTask?.tag)
        assertEquals("#FF0000", fakeTaskManager.lastUpdatedTask?.tagColor)
    }

    @Test
    fun `saveTask creates task with canonicalized tag`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("New Task")
        viewModel.updateTag("  work  ")

        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("WORK", fakeTaskManager.lastCreatedTag)
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

    // === dueAtHasTime Tests ===

    @Test
    fun `dueAtHasTime defaults to false`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `updateDueTime sets dueAtHasTime to true`() {
        val viewModel = createViewModel()
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateDueAt(futureDate)

        viewModel.updateDueTime(14, 30)

        assertTrue(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `updateDueTime updates dueAt with correct hour and minute`() {
        val viewModel = createViewModel()
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateDueAt(futureDate)

        viewModel.updateDueTime(14, 30)

        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = viewModel.dueAt.value!!
        }
        assertEquals(14, calendar.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(java.util.Calendar.MINUTE))
        assertEquals(0, calendar.get(java.util.Calendar.SECOND))
    }

    @Test
    fun `updateDueTime does nothing when dueAt is null`() {
        val viewModel = createViewModel()
        viewModel.updateDueTime(14, 30)

        assertNull(viewModel.dueAt.value)
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `updateDueTime clears stale reminder`() {
        val viewModel = createViewModel()
        // Set due date far in the future so the 1-day reminder is valid initially
        val futureDate = System.currentTimeMillis() + 86400000L * 3
        viewModel.updateDueAt(futureDate)
        viewModel.updateReminderOption(ReminderOption.DAYS_1)
        assertEquals(ReminderOption.DAYS_1, viewModel.reminderOption.value)

        // Now change the due date to today — a 1-day reminder offset would be in the past
        val today = System.currentTimeMillis() + 60000L // 1 minute from now
        viewModel.updateDueAt(today)
        // Set time close to now — the 1-day (1440 min) offset puts reminder well in the past
        val now = java.util.Calendar.getInstance()
        viewModel.updateDueTime(now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE))

        // Reminder should be cleared since 1-day offset would be in the past
        assertEquals(ReminderOption.NONE, viewModel.reminderOption.value)
    }

    @Test
    fun `clearDueTime resets to 23 59 and sets dueAtHasTime to false`() {
        val viewModel = createViewModel()
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateDueAt(futureDate)
        viewModel.updateDueTime(14, 30)
        assertTrue(viewModel.dueAtHasTime.value)

        viewModel.clearDueTime()

        assertFalse(viewModel.dueAtHasTime.value)
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = viewModel.dueAt.value!!
        }
        assertEquals(23, calendar.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun `clearDueTime does nothing when dueAt is null`() {
        val viewModel = createViewModel()
        viewModel.clearDueTime()
        assertNull(viewModel.dueAt.value)
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `setDueDatePreset sets dueAtHasTime to false`() {
        val viewModel = createViewModel()
        // First set a time
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateDueAt(futureDate)
        viewModel.updateDueTime(14, 30)
        assertTrue(viewModel.dueAtHasTime.value)

        // Preset should clear hasTime
        viewModel.setDueDatePreset(DueDatePreset.TOMORROW)
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `clearDueDate resets dueAtHasTime to false`() {
        val viewModel = createViewModel()
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateDueAt(futureDate)
        viewModel.updateDueTime(14, 30)

        viewModel.clearDueDate()

        assertNull(viewModel.dueAt.value)
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `updateDueAt with null resets dueAtHasTime`() {
        val viewModel = createViewModel()
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateDueAt(futureDate)
        viewModel.updateDueTime(14, 30)
        assertTrue(viewModel.dueAtHasTime.value)

        viewModel.updateDueAt(null)
        assertFalse(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `edit mode - loads dueAtHasTime from existing task`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Meeting",
            dueAt = System.currentTimeMillis() + 86400000L,
            dueAtHasTime = true,
        )
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.dueAtHasTime.value)
    }

    @Test
    fun `edit mode - hasChanges detects dueAtHasTime change`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Meeting",
            dueAt = System.currentTimeMillis() + 86400000L,
            dueAtHasTime = false,
        )
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.hasChanges.value)

        viewModel.updateDueTime(14, 30)
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `saveTask passes dueAtHasTime in create mode`() = runTest {
        val viewModel = createViewModel()
        val futureDate = System.currentTimeMillis() + 86400000L
        viewModel.updateTitle("Task")
        viewModel.updateDueAt(futureDate)
        viewModel.updateDueTime(14, 30)

        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeTaskManager.createCalled)
        assertTrue(fakeTaskManager.lastCreatedDueAtHasTime)
    }

    @Test
    fun `saveTask passes dueAtHasTime in edit mode`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Meeting",
            dueAt = System.currentTimeMillis() + 86400000L,
            dueAtHasTime = false,
        )
        fakeTaskManager.taskToReturn = task

        val viewModel = createViewModel(taskId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDueTime(14, 30)
        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeTaskManager.updateCalled)
        assertTrue(fakeTaskManager.lastUpdatedTask!!.dueAtHasTime)
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

    // === Subtask editor tests ===

    @Test
    fun `addSubtaskDraft appends a draft with a negative id`() = runTest {
        val viewModel = createViewModel()

        viewModel.addSubtaskDraft("Buy milk")

        val drafts = viewModel.subtasks.value
        assertEquals(1, drafts.size)
        assertEquals("Buy milk", drafts[0].title)
        assertTrue(drafts[0].id < 0L)
        assertFalse(drafts[0].isPersisted)
        assertEquals(0, drafts[0].sortOrder)
    }

    @Test
    fun `addSubtaskDraft ignores blank titles`() = runTest {
        val viewModel = createViewModel()

        viewModel.addSubtaskDraft("   ")

        assertTrue(viewModel.subtasks.value.isEmpty())
    }

    @Test
    fun `addSubtaskDraft rejects titles over MAX_TITLE_LENGTH`() = runTest {
        val viewModel = createViewModel()
        val longTitle = "a".repeat(501)

        viewModel.addSubtaskDraft(longTitle)

        assertTrue(viewModel.subtasks.value.isEmpty())
    }

    @Test
    fun `toggleSubtaskDraft flips isCompleted`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        val id = viewModel.subtasks.value.first().id

        viewModel.toggleSubtaskDraft(id)
        assertTrue(viewModel.subtasks.value.first().isCompleted)

        viewModel.toggleSubtaskDraft(id)
        assertFalse(viewModel.subtasks.value.first().isCompleted)
    }

    @Test
    fun `removeSubtaskDraft drops the row and reindexes sortOrder`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        viewModel.addSubtaskDraft("C")
        val middleId = viewModel.subtasks.value[1].id

        viewModel.removeSubtaskDraft(middleId)

        val after = viewModel.subtasks.value
        assertEquals(listOf("A", "C"), after.map { it.title })
        assertEquals(listOf(0, 1), after.map { it.sortOrder })
    }

    @Test
    fun `adding a subtask enables isSaveEnabled in create mode with title`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        val baseline = viewModel.isSaveEnabled.first()
        assertTrue(baseline)

        viewModel.addSubtaskDraft("step")

        // Still enabled (title was already valid); and hasChanges stays true.
        assertTrue(viewModel.hasChanges.value)
        assertTrue(viewModel.isSaveEnabled.first())
    }

    @Test
    fun `adding only a subtask with no title leaves isSaveEnabled false`() = runTest {
        val viewModel = createViewModel()

        viewModel.addSubtaskDraft("step")

        assertTrue(viewModel.hasChanges.value) // hasChanges reflects subtask presence
        // But isSaveEnabled requires a valid title too.
        assertFalse(viewModel.isSaveEnabled.first())
    }

    @Test
    fun `saveTask persists draft subtasks via the use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        viewModel.addSubtaskDraft("First")
        viewModel.addSubtaskDraft("Second")

        viewModel.saveTask()

        val written = subtaskRepository.getAllSubtasksSnapshot()
        assertEquals(2, written.size)
        assertEquals(listOf("First", "Second"), written.map { it.title })
        assertTrue(written.all { it.taskId == 1L }) // FakeEditorTaskManager returns 1L
        assertTrue(written.all { !it.isCompleted })
    }

    @Test
    fun `saveTask trims subtask titles before persisting`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        // The add path trims, but direct state mutation via updateSubtaskDraftTitle does not.
        viewModel.addSubtaskDraft("keep")
        val id = viewModel.subtasks.value.first().id
        viewModel.updateSubtaskDraftTitle(id, "  padded  ")

        viewModel.saveTask()

        val written = subtaskRepository.getAllSubtasksSnapshot()
        assertEquals(1, written.size)
        assertEquals("padded", written.first().title)
    }

    @Test
    fun `saveTask skips blank-title drafts and surfaces an error`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        viewModel.addSubtaskDraft("valid")
        val id = viewModel.subtasks.value.first().id
        // Simulate the user clearing the title of a draft row before saving.
        viewModel.updateSubtaskDraftTitle(id, "")

        viewModel.saveTask()

        // Blank-title draft dropped from the write.
        assertTrue(subtaskRepository.getAllSubtasksSnapshot().isEmpty())
        // Error surfaced to the UI.
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun `saveTask with only valid drafts does not set errorMessage`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        viewModel.addSubtaskDraft("valid")

        viewModel.saveTask()

        assertNull(viewModel.errorMessage.value)
    }

    // === Drag-reorder tests ===

    @Test
    fun `moveSubtaskDraft reorders the list and reindexes sortOrder`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        viewModel.addSubtaskDraft("C")

        // Move A (index 0) to the end (index 2).
        viewModel.moveSubtaskDraft(fromIndex = 0, toIndex = 2)

        val after = viewModel.subtasks.value
        assertEquals(listOf("B", "C", "A"), after.map { it.title })
        assertEquals(listOf(0, 1, 2), after.map { it.sortOrder })
    }

    @Test
    fun `moveSubtaskDraft is a no-op when from equals to`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        val before = viewModel.subtasks.value

        viewModel.moveSubtaskDraft(fromIndex = 1, toIndex = 1)

        assertEquals(before, viewModel.subtasks.value)
    }

    @Test
    fun `moveSubtaskDraft ignores out-of-bounds indices`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        val before = viewModel.subtasks.value

        viewModel.moveSubtaskDraft(fromIndex = 0, toIndex = 5)
        viewModel.moveSubtaskDraft(fromIndex = -1, toIndex = 0)

        assertEquals(before, viewModel.subtasks.value)
    }

    @Test
    fun `moveSubtaskDraft marks hasChanges`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        // addSubtaskDraft already sets hasChanges, but we want to assert move alone would also do it.
        // Seed an "unchanged" baseline by checking hasChanges is already true here.
        assertTrue(viewModel.hasChanges.value)

        viewModel.moveSubtaskDraft(fromIndex = 0, toIndex = 1)
        assertTrue(viewModel.hasChanges.value)
    }

    @Test
    fun `moveSubtaskDraftBy +1 swaps a draft with the next row`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        viewModel.addSubtaskDraft("C")
        val midId = viewModel.subtasks.value[1].id // "B"

        viewModel.moveSubtaskDraftBy(draftId = midId, direction = 1)

        assertEquals(listOf("A", "C", "B"), viewModel.subtasks.value.map { it.title })
    }

    @Test
    fun `moveSubtaskDraftBy -1 swaps a draft with the previous row`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        val lastId = viewModel.subtasks.value[1].id

        viewModel.moveSubtaskDraftBy(draftId = lastId, direction = -1)

        assertEquals(listOf("B", "A"), viewModel.subtasks.value.map { it.title })
    }

    @Test
    fun `moveSubtaskDraftBy returns true on successful move`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        val id = viewModel.subtasks.value[0].id

        assertTrue(viewModel.moveSubtaskDraftBy(draftId = id, direction = 1))
    }

    @Test
    fun `moveSubtaskDraftBy returns false and no-ops at list boundaries`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        val firstId = viewModel.subtasks.value[0].id
        val lastId = viewModel.subtasks.value[1].id
        val before = viewModel.subtasks.value

        // Top row can't move up.
        assertFalse(viewModel.moveSubtaskDraftBy(draftId = firstId, direction = -1))
        // Bottom row can't move down.
        assertFalse(viewModel.moveSubtaskDraftBy(draftId = lastId, direction = 1))

        assertEquals(before, viewModel.subtasks.value)
    }

    @Test
    fun `moveSubtaskDraftBy returns false for unknown id`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        val before = viewModel.subtasks.value

        assertFalse(viewModel.moveSubtaskDraftBy(draftId = 9999L, direction = 1))

        assertEquals(before, viewModel.subtasks.value)
    }

    @Test
    fun `moveSubtaskDraftBy returns false for invalid direction`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        val id = viewModel.subtasks.value[0].id
        val before = viewModel.subtasks.value

        assertFalse(viewModel.moveSubtaskDraftBy(draftId = id, direction = 2))
        assertFalse(viewModel.moveSubtaskDraftBy(draftId = id, direction = 0))

        assertEquals(before, viewModel.subtasks.value)
    }

    // === Save-path reorder tests ===

    @Test
    fun `saveTask persists sortOrder reorder via the use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        viewModel.addSubtaskDraft("C")

        viewModel.saveTask()

        // After first save the drafts get real ids; FakeEditorTaskManager returns taskId = 1L.
        val persistedOrder = subtaskRepository.getSubtasks(1L).map { it.title }
        assertEquals(listOf("A", "B", "C"), persistedOrder)
        assertEquals(listOf(0, 1, 2), subtaskRepository.getSubtasks(1L).map { it.sortOrder })
    }

    @Test
    fun `saveTask in create mode with reorder writes drafts in the reordered position`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateTitle("Parent")
        viewModel.addSubtaskDraft("A")
        viewModel.addSubtaskDraft("B")
        viewModel.addSubtaskDraft("C")

        // Reorder BEFORE save: move A (index 0) to the end.
        viewModel.moveSubtaskDraft(fromIndex = 0, toIndex = 2)

        viewModel.saveTask()

        // Because drafts are saved in their final list order, sortOrder matches the reordered
        // list even before the explicit reorder tail pass runs.
        val persisted = subtaskRepository.getSubtasks(1L).sortedBy { it.sortOrder }
        assertEquals(listOf("B", "C", "A"), persisted.map { it.title })
        assertEquals(listOf(0, 1, 2), persisted.map { it.sortOrder })
    }

    @Test
    fun `saveTask in edit mode persists sortOrder change via reorder tail`() = runTest {
        // Seed the fake with a persisted task and three persisted subtasks.
        val task = TestTaskFactory.createTask(id = 42L, title = "Parent")
        fakeTaskManager.taskToReturn = task
        subtaskRepository.seed(
            dev.tuandoan.tasktracker.testutil.TestSubtaskFactory.createSubtask(
                id = 10L,
                taskId = 42L,
                title = "A",
                sortOrder = 0,
            ),
            dev.tuandoan.tasktracker.testutil.TestSubtaskFactory.createSubtask(
                id = 11L,
                taskId = 42L,
                title = "B",
                sortOrder = 1,
            ),
            dev.tuandoan.tasktracker.testutil.TestSubtaskFactory.createSubtask(
                id = 12L,
                taskId = 42L,
                title = "C",
                sortOrder = 2,
            ),
        )

        // Open in edit mode so loadTask seeds originalSubtasks from the fake.
        val viewModel = createViewModel(taskId = 42L)
        // The `init` path may launch asynchronously; let it settle.
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isEditMode)
        assertEquals(listOf("A", "B", "C"), viewModel.subtasks.value.map { it.title })

        // Reorder: move the top draft to the end.
        viewModel.moveSubtaskDraftBy(draftId = 10L, direction = 1)
        viewModel.moveSubtaskDraftBy(draftId = 10L, direction = 1)

        viewModel.saveTask()
        testDispatcher.scheduler.advanceUntilIdle()

        // Persisted sortOrder reflects the reordered list (B, C, A).
        val persisted = subtaskRepository.getSubtasks(42L)
        assertEquals(listOf("B", "C", "A"), persisted.map { it.title })
        assertEquals(listOf(0, 1, 2), persisted.map { it.sortOrder })
    }
}

private class FakeEditorTaskManager : ITaskManager {

    var taskToReturn: Task? = null
    var createCalled = false
    var updateCalled = false
    var lastCreatedTitle: String? = null
    var lastCreatedDueAtHasTime: Boolean = false
    var lastCreatedTag: String? = null
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
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Long {
        createCalled = true
        lastCreatedTitle = title
        lastCreatedDueAtHasTime = dueAtHasTime
        return 1L
    }

    override suspend fun createTask(
        title: String,
        description: String,
        dueAt: Long?,
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
        recurrenceType: Int,
        recurrenceInterval: Int,
        recurrenceDaysOfWeek: Int,
        recurrenceEndDate: Long?,
    ): Long {
        createCalled = true
        lastCreatedTitle = title
        lastCreatedDueAtHasTime = dueAtHasTime
        lastCreatedTag = tag
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
        dueAtHasTime: Boolean,
        reminderOffsetMinutes: Int?,
        tag: String?,
    ): Boolean = true

    override suspend fun deleteTask(task: Task) {}
    override suspend fun restoreTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTaskCompletion(task: Task) {}
    override suspend fun markTaskComplete(task: Task) {}
    override suspend fun markTaskIncomplete(task: Task) {}
    override suspend fun skipOccurrence(task: Task) {}
    override suspend fun setCompletedBulk(ids: List<Long>, completed: Boolean) {}
    override suspend fun deleteTasksByIds(ids: List<Long>) {}
    override suspend fun restoreTasks(tasks: List<Task>): Result<Unit> = Result.success(Unit)
    override fun getActiveTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override fun getCompletedTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun setPinned(taskId: Long, pinned: Boolean) {}
    override suspend fun setPriority(taskId: Long, priority: Int) {}
    override suspend fun setPriorityBulk(ids: List<Long>, priority: Int) {}
    override suspend fun setTagBulk(ids: List<Long>, tag: String?, tagColor: String?) {}
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

    override suspend fun materializeProjectedOccurrence(parentId: Long, date: LocalDate, zone: ZoneId): Long? = null
}
