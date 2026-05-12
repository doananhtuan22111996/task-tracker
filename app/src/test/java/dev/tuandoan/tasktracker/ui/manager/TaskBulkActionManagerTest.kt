package dev.tuandoan.tasktracker.ui.manager

import android.content.Context
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.fakeAnalyticsLogger
import dev.tuandoan.tasktracker.testutil.fakeBreadcrumbLogger
import dev.tuandoan.tasktracker.ui.state.TaskFormStateManager
import dev.tuandoan.tasktracker.ui.state.TaskSelectionStateManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskBulkActionManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var repository: FakeTaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var selectionManager: TaskSelectionStateManager
    private lateinit var crudManager: TaskCrudManager
    private lateinit var analyticsLogger: dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
    private lateinit var bulkManager: TaskBulkActionManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        // Mock getString with varargs (resId, Object... formatArgs)
        // args[0] = resId (Int), args[1] = Object[] (the varargs array)
        every { context.getString(any<Int>(), *anyVararg()) } answers {
            val resId = firstArg<Int>()
            val varargs = secondArg<Array<out Any?>>()
            val firstFormatArg = varargs.firstOrNull()
            when (resId) {
                R.string.snackbar_tasks_marked_completed -> "$firstFormatArg tasks marked as completed"
                R.string.snackbar_tasks_marked_active -> "$firstFormatArg tasks marked as active"
                R.string.snackbar_failed_delete_tasks -> "Failed to delete tasks: $firstFormatArg"
                R.string.snackbar_tasks_archived -> "$firstFormatArg tasks archived"
                R.string.snackbar_failed_archive_tasks -> "Failed to archive tasks: $firstFormatArg"
                R.string.snackbar_tasks_restored -> "$firstFormatArg tasks restored from archive"
                R.string.snackbar_failed_restore_tasks -> "Failed to restore tasks: $firstFormatArg"
                R.string.snackbar_failed_restore_archive -> "Failed to restore tasks from archive: $firstFormatArg"
                R.string.snackbar_tasks_permanently_deleted -> "$firstFormatArg tasks permanently deleted"
                R.string.snackbar_failed_permanent_delete -> "Failed to permanently delete tasks: $firstFormatArg"
                R.string.snackbar_tasks_priority_updated -> "Priority updated for $firstFormatArg tasks"
                R.string.snackbar_tasks_tagged -> "Tag applied to $firstFormatArg tasks"
                R.string.snackbar_tasks_tag_cleared -> "Tag cleared from $firstFormatArg tasks"
                else -> "unknown string $resId"
            }
        }
        // Mock getString without varargs (single-arg overload)
        every { context.getString(R.string.snackbar_failed_mark_completed) } returns "Failed to mark tasks as completed"
        every { context.getString(R.string.snackbar_failed_mark_active) } returns "Failed to mark tasks as active"
        every { context.getString(R.string.error_update_tasks) } returns "Failed to update tasks"
        every { context.getString(R.string.error_validation_failed) } returns "Validation failed"

        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        val taskManager =
            TaskManager(
                repository,
                FakeSubtaskRepository(),
                scheduler,
                FakeWidgetUpdater(),
                fakeBreadcrumbLogger(),
                fakeAnalyticsLogger(),
            )
        val crudUseCase = TaskCrudUseCase(taskManager, context)
        val formUseCase = TaskFormUseCase(context)
        val formStateManager = TaskFormStateManager(formUseCase, context)
        selectionManager = TaskSelectionStateManager()
        crudManager = TaskCrudManager(crudUseCase, formStateManager, context)
        analyticsLogger = mockk(relaxed = true)
        bulkManager = TaskBulkActionManager(context, crudManager, selectionManager, analyticsLogger)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // === requestBulkDelete ===

    @Test
    fun `requestBulkDelete with single selection sets pending tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        selectionManager.enterSelection(2L)

        bulkManager.requestBulkDelete(tasks)

        assertTrue(bulkManager.hasPendingBulkDelete())
        assertEquals(1, bulkManager.getPendingDeleteCount())
        assertEquals(2L, bulkManager.pendingBulkDeleteTasks.value[0].id)
    }

    @Test
    fun `requestBulkDelete with multiple selections sets pending tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        selectionManager.selectAll(listOf(1L, 3L))

        bulkManager.requestBulkDelete(tasks)

        assertTrue(bulkManager.hasPendingBulkDelete())
        assertEquals(2, bulkManager.getPendingDeleteCount())
    }

    @Test
    fun `requestBulkDelete with empty selection does nothing`() {
        val tasks = TestTaskFactory.createTaskList(3)

        bulkManager.requestBulkDelete(tasks)

        assertFalse(bulkManager.hasPendingBulkDelete())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requestBulkDelete with empty task list throws`() {
        selectionManager.enterSelection(1L)
        bulkManager.requestBulkDelete(emptyList())
    }

    @Test(expected = IllegalStateException::class)
    fun `requestBulkDelete with missing selected task throws`() {
        val tasks = TestTaskFactory.createTaskList(3)
        selectionManager.enterSelection(999L)

        bulkManager.requestBulkDelete(tasks)
    }

    // === cancelBulkDelete ===

    @Test
    fun `cancelBulkDelete clears pending tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        selectionManager.enterSelection(1L)
        bulkManager.requestBulkDelete(tasks)

        bulkManager.cancelBulkDelete()

        assertFalse(bulkManager.hasPendingBulkDelete())
    }

    // === confirmBulkDelete ===

    @Test
    fun `confirmBulkDelete removes tasks and clears selection`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))
        bulkManager.requestBulkDelete(tasks)

        bulkManager.confirmBulkDelete(this)
        advanceUntilIdle()

        assertFalse(bulkManager.hasPendingBulkDelete())
        assertFalse(selectionManager.hasSelection())
    }

    @Test(expected = IllegalStateException::class)
    fun `confirmBulkDelete without pending throws`() = runTest {
        bulkManager.confirmBulkDelete(this)
    }

    // === requestBulkArchive ===

    @Test
    fun `requestBulkArchive with selection sets pending archive tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        selectionManager.enterSelection(1L)

        bulkManager.requestBulkArchive(tasks)

        assertEquals(1, bulkManager.pendingBulkArchiveTasks.value.size)
    }

    @Test
    fun `requestBulkArchive with empty selection does nothing`() {
        val tasks = TestTaskFactory.createTaskList(3)

        bulkManager.requestBulkArchive(tasks)

        assertTrue(bulkManager.pendingBulkArchiveTasks.value.isEmpty())
    }

    // === cancelBulkArchive ===

    @Test
    fun `cancelBulkArchive clears pending archive tasks`() {
        val tasks = TestTaskFactory.createTaskList(3)
        selectionManager.enterSelection(1L)
        bulkManager.requestBulkArchive(tasks)

        bulkManager.cancelBulkArchive()

        assertTrue(bulkManager.pendingBulkArchiveTasks.value.isEmpty())
    }

    // === confirmBulkArchive ===

    @Test
    fun `confirmBulkArchive archives tasks and clears selection`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))
        bulkManager.requestBulkArchive(tasks)

        bulkManager.confirmBulkArchive(this)
        advanceUntilIdle()

        assertTrue(bulkManager.pendingBulkArchiveTasks.value.isEmpty())
        assertFalse(selectionManager.hasSelection())
    }

    @Test(expected = IllegalStateException::class)
    fun `confirmBulkArchive without pending throws`() = runTest {
        bulkManager.confirmBulkArchive(this)
    }

    // === bulkMarkCompleted ===

    @Test
    fun `bulkMarkCompleted with selection marks tasks completed and clears selection`() = runTest {
        val tasks = TestTaskFactory.createTaskList(2)
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))

        var successMsg = ""
        bulkManager.bulkMarkCompleted(this, onSuccess = { successMsg = it })
        advanceUntilIdle()

        assertFalse(selectionManager.hasSelection())
        assertTrue(successMsg.contains("2 tasks"))
    }

    @Test
    fun `bulkMarkCompleted with no selection calls onError`() = runTest {
        var errorMsg = ""
        bulkManager.bulkMarkCompleted(this, onError = { errorMsg = it })
        advanceUntilIdle()

        assertTrue(errorMsg.contains("No tasks selected"))
    }

    // === bulkMarkActive ===

    @Test
    fun `bulkMarkActive with selection marks tasks active`() = runTest {
        val tasks = listOf(
            TestTaskFactory.completedTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))

        var successMsg = ""
        bulkManager.bulkMarkActive(this, onSuccess = { successMsg = it })
        advanceUntilIdle()

        assertTrue(successMsg.contains("2 tasks"))
    }

    // === bulkApplyPriority ===

    @Test
    fun `bulkApplyPriority sets priority on all selected tasks and clears selection`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, priority = 1),
            TestTaskFactory.createTask(id = 2, priority = 1),
            TestTaskFactory.createTask(id = 3, priority = 1),
        )
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))

        var successMsg = ""
        bulkManager.bulkApplyPriority(this, priority = 2, onSuccess = { successMsg = it })
        advanceUntilIdle()

        assertTrue(successMsg.contains("2"))
        val snapshot = repository.getAllTasksSnapshot()
        assertEquals(2, snapshot.first { it.id == 1L }.priority)
        assertEquals(2, snapshot.first { it.id == 2L }.priority)
        // Unselected task untouched.
        assertEquals(1, snapshot.first { it.id == 3L }.priority)
        // Selection cleared on success.
        assertTrue(selectionManager.selectedIds.value.isEmpty())
    }

    @Test
    fun `bulkApplyPriority rejects out-of-range priority`() = runTest {
        try {
            bulkManager.bulkApplyPriority(this, priority = 3)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    // === bulkApplyTag ===

    @Test
    fun `bulkApplyTag sets tag and color on selected tasks and clears selection`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.createTask(id = 2),
            TestTaskFactory.createTask(id = 3),
        )
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))

        var successMsg = ""
        bulkManager.bulkApplyTag(this, tag = "work", tagColor = "blue", onSuccess = { successMsg = it })
        advanceUntilIdle()

        assertTrue(successMsg.contains("Tag applied"))
        val snapshot = repository.getAllTasksSnapshot()
        // Tag is normalized to uppercase by TagNormalizer at the manager layer.
        assertEquals("WORK", snapshot.first { it.id == 1L }.tag)
        assertEquals("blue", snapshot.first { it.id == 1L }.tagColor)
        assertEquals("WORK", snapshot.first { it.id == 2L }.tag)
        // Unselected untouched.
        assertEquals(null, snapshot.first { it.id == 3L }.tag)
        // Selection cleared.
        assertTrue(selectionManager.selectedIds.value.isEmpty())
    }

    @Test
    fun `bulkApplyTag with null tag clears tag and color on selected tasks`() = runTest {
        // Pre-seed tasks with existing tags; factory doesn't expose tagColor so set it via copy.
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, tag = "work").copy(tagColor = "blue"),
            TestTaskFactory.createTask(id = 2, tag = "personal").copy(tagColor = "green"),
        )
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))

        var successMsg = ""
        bulkManager.bulkApplyTag(this, tag = null, tagColor = null, onSuccess = { successMsg = it })
        advanceUntilIdle()

        assertTrue(successMsg.contains("Tag cleared"))
        val snapshot = repository.getAllTasksSnapshot()
        assertEquals(null, snapshot.first { it.id == 1L }.tag)
        assertEquals(null, snapshot.first { it.id == 1L }.tagColor)
        assertEquals(null, snapshot.first { it.id == 2L }.tag)
        assertEquals(null, snapshot.first { it.id == 2L }.tagColor)
    }

    @Test
    fun `bulkApplyTag with blank tag treats it as clear`() = runTest {
        val tasks = listOf(TestTaskFactory.createTask(id = 1, tag = "work").copy(tagColor = "blue"))
        repository.seed(*tasks.toTypedArray())
        selectionManager.enterSelection(1L)

        var successMsg = ""
        bulkManager.bulkApplyTag(this, tag = "   ", tagColor = "red", onSuccess = { successMsg = it })
        advanceUntilIdle()

        // Blank tags are normalized to null; routed as clear.
        assertTrue(successMsg.contains("Tag cleared"))
        val snapshot = repository.getAllTasksSnapshot()
        assertEquals(null, snapshot.first { it.id == 1L }.tag)
        assertEquals(null, snapshot.first { it.id == 1L }.tagColor)
    }

    // === requestBulkPermanentDelete ===

    @Test
    fun `requestBulkPermanentDelete sets pending delete for archived tasks`() {
        val tasks = listOf(
            TestTaskFactory.archivedTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        selectionManager.enterSelection(1L)

        bulkManager.requestBulkPermanentDelete(tasks)

        assertTrue(bulkManager.hasPendingBulkDelete())
    }

    // === confirmBulkPermanentDelete ===

    @Test
    fun `confirmBulkPermanentDelete permanently removes tasks`() = runTest {
        val tasks = listOf(
            TestTaskFactory.archivedTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))
        bulkManager.requestBulkPermanentDelete(tasks)

        bulkManager.confirmBulkPermanentDelete(this)
        advanceUntilIdle()

        assertFalse(bulkManager.hasPendingBulkDelete())
        assertFalse(selectionManager.hasSelection())
    }

    @Test(expected = IllegalStateException::class)
    fun `confirmBulkPermanentDelete without pending throws`() = runTest {
        bulkManager.confirmBulkPermanentDelete(this)
    }

    // === bulkRestoreArchived ===

    @Test
    fun `bulkRestoreArchived with selection restores and clears selection`() = runTest {
        val tasks = listOf(
            TestTaskFactory.archivedTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())
        selectionManager.selectAll(listOf(1L, 2L))

        var successMsg = ""
        bulkManager.bulkRestoreArchived(this, onSuccess = { successMsg = it })
        advanceUntilIdle()

        assertTrue(successMsg.contains("2 tasks"))
    }

    // FB-14: BulkOperationApplied is tested at the AnalyticsEvent-shape level in
    // AnalyticsLoggerTest (FB-13). The integration here (bulkMarkCompleted fires the right
    // op_type through executeBulkOperation) is device-verified via Firebase Analytics debug
    // view; the selection-state-flow initialization this test needs is heavy for a JVM unit.
}
