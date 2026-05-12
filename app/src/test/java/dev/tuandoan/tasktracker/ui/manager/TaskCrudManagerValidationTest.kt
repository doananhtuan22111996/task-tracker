package dev.tuandoan.tasktracker.ui.manager

import android.content.Context
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.usecase.TaskCrudUseCase
import dev.tuandoan.tasktracker.domain.usecase.TaskFormUseCase
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import dev.tuandoan.tasktracker.testutil.fakeBreadcrumbLogger
import dev.tuandoan.tasktracker.ui.state.TaskFormStateManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskCrudManagerValidationTest {

    private lateinit var context: Context
    private lateinit var repository: FakeTaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var taskManager: TaskManager
    private lateinit var crudUseCase: TaskCrudUseCase
    private lateinit var formUseCase: TaskFormUseCase
    private lateinit var formStateManager: TaskFormStateManager
    private lateinit var crudManager: TaskCrudManager

    @Before
    fun setup() {
        context = mockk(relaxed = true) {
            every { getString(any()) } returns "test string"
            every { getString(any(), *anyVararg()) } returns "test string"
        }
        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        taskManager =
            TaskManager(repository, FakeSubtaskRepository(), scheduler, FakeWidgetUpdater(), fakeBreadcrumbLogger())
        crudUseCase = TaskCrudUseCase(taskManager, context)
        formUseCase = TaskFormUseCase(context)
        formStateManager = TaskFormStateManager(formUseCase, context)
        crudManager = TaskCrudManager(crudUseCase, formStateManager, context)
    }

    // === bulkMarkCompleted input validation ===

    @Test
    fun `bulkMarkCompleted with empty ids returns validation error`() = runTest {
        val result = crudManager.bulkMarkCompleted(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkMarkCompleted with invalid ids returns validation error`() = runTest {
        val result = crudManager.bulkMarkCompleted(listOf(-1L, 0L))
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkMarkCompleted with duplicate ids returns validation error`() = runTest {
        val result = crudManager.bulkMarkCompleted(listOf(1L, 1L))
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkMarkCompleted with valid ids succeeds`() = runTest {
        val tasks = TestTaskFactory.createTaskList(2)
        repository.seed(*tasks.toTypedArray())

        val result = crudManager.bulkMarkCompleted(listOf(1L, 2L))
        assertTrue(result is TaskOperationResult.Success)
    }

    // === bulkMarkActive input validation ===

    @Test
    fun `bulkMarkActive with empty ids returns validation error`() = runTest {
        val result = crudManager.bulkMarkActive(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkMarkActive with valid ids succeeds`() = runTest {
        val tasks = listOf(
            TestTaskFactory.completedTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        val result = crudManager.bulkMarkActive(listOf(1L, 2L))
        assertTrue(result is TaskOperationResult.Success)
    }

    // === bulkDeleteTasks input validation ===

    @Test
    fun `bulkDeleteTasks with empty ids returns validation error`() = runTest {
        val result = crudManager.bulkDeleteTasks(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkDeleteTasks with valid ids succeeds`() = runTest {
        val tasks = TestTaskFactory.createTaskList(2)
        repository.seed(*tasks.toTypedArray())

        val result = crudManager.bulkDeleteTasks(listOf(1L, 2L))
        assertTrue(result is TaskOperationResult.Success)
    }

    // === restoreTasks input validation ===

    @Test
    fun `restoreTasks with empty list returns validation error`() = runTest {
        val result = crudManager.restoreTasks(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `restoreTasks with invalid task ids returns validation error`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 0),
        )
        val result = crudManager.restoreTasks(tasks)
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `restoreTasks with duplicate tasks returns validation error`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        val result = crudManager.restoreTasks(listOf(task, task))
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    // === toggleTaskCompletion ===

    @Test
    fun `toggleTaskCompletion succeeds for existing task`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = crudManager.toggleTaskCompletion(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    // === deleteTask ===

    @Test
    fun `deleteTask succeeds`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = crudManager.deleteTask(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    // === restoreTask ===

    @Test
    fun `restoreTask succeeds`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)

        val result = crudManager.restoreTask(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    // === toggleTaskPin ===

    @Test
    fun `toggleTaskPin succeeds`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = crudManager.toggleTaskPin(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    // === archive operations ===

    @Test
    fun `archiveTask succeeds`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = crudManager.archiveTask(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    @Test
    fun `unarchiveTask succeeds`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 1)
        repository.seed(task)

        val result = crudManager.unarchiveTask(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    @Test
    fun `hardDeleteTask succeeds`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 1)
        repository.seed(task)

        val result = crudManager.hardDeleteTask(task)
        assertTrue(result is TaskOperationResult.Success)
    }

    @Test
    fun `bulkArchiveTasks with empty ids returns validation error`() = runTest {
        val result = crudManager.bulkArchiveTasks(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkUnarchiveTasks with empty ids returns validation error`() = runTest {
        val result = crudManager.bulkUnarchiveTasks(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `bulkHardDeleteTasks with empty ids returns validation error`() = runTest {
        val result = crudManager.bulkHardDeleteTasks(emptyList())
        assertTrue(result is TaskOperationResult.ValidationError)
    }

    @Test
    fun `exceeding max bulk operation size returns validation error`() = runTest {
        val ids = (1L..501L).toList()
        val result = crudManager.bulkMarkCompleted(ids)
        assertTrue(result is TaskOperationResult.ValidationError)
        assertTrue((result as TaskOperationResult.ValidationError).message.contains("500"))
    }

    // === TaskOperationResult sealed class ===

    @Test
    fun `TaskOperationResult Success holds message`() {
        val result = TaskOperationResult.Success("Done")
        assertEquals("Done", result.message)
    }

    @Test
    fun `TaskOperationResult ValidationError holds message`() {
        val result = TaskOperationResult.ValidationError("Bad input")
        assertEquals("Bad input", result.message)
    }

    @Test
    fun `TaskOperationResult CrudError holds message`() {
        val result = TaskOperationResult.CrudError("DB failed")
        assertEquals("DB failed", result.message)
    }
}
