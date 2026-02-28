package dev.tuandoan.tasktracker.ui.manager

import android.content.Context
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
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
                else -> "unknown string $resId"
            }
        }
        // Mock getString without varargs (single-arg overload)
        every { context.getString(R.string.snackbar_failed_mark_completed) } returns "Failed to mark tasks as completed"
        every { context.getString(R.string.snackbar_failed_mark_active) } returns "Failed to mark tasks as active"

        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        val taskManager = TaskManager(repository, scheduler)
        val crudUseCase = TaskCrudUseCase(taskManager, context)
        val formUseCase = TaskFormUseCase(context)
        val formStateManager = TaskFormStateManager(formUseCase, context)
        selectionManager = TaskSelectionStateManager()
        crudManager = TaskCrudManager(crudUseCase, formStateManager, context)
        bulkManager = TaskBulkActionManager(context, crudManager, selectionManager)
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
}
