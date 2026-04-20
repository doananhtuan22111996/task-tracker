package dev.tuandoan.tasktracker.domain.usecase

import android.content.Context
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskCrudUseCaseTest {

    private lateinit var context: Context
    private lateinit var repository: FakeTaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var taskManager: TaskManager
    private lateinit var useCase: TaskCrudUseCase

    @Before
    fun setup() {
        context = mockk(relaxed = true) {
            every { getString(any()) } returns "test string"
            every { getString(any(), *anyVararg()) } returns "test string"
        }
        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        taskManager = TaskManager(repository, scheduler, FakeWidgetUpdater())
        useCase = TaskCrudUseCase(taskManager, context)
    }

    // === createTask ===

    @Test
    fun `createTask with valid input succeeds`() = runTest {
        val result = useCase.createTask("Buy milk", "From store", null, false, null, null)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.getAllTasksSnapshot().size)
    }

    @Test
    fun `createTask with blank title fails`() = runTest {
        val result = useCase.createTask("   ", "", null, false, null, null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `createTask with tag stores tag`() = runTest {
        val result = useCase.createTask("Task", "", null, false, null, "work")

        assertTrue(result.isSuccess)
        // Tag is canonicalized to uppercase at TaskManager boundary
        val tasks = repository.getAllTasksSnapshot()
        assertEquals(1, tasks.size)
        assertEquals("WORK", tasks[0].tag)
    }

    // === updateTask ===

    @Test
    fun `updateTask with valid input succeeds`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Old")
        repository.seed(task)

        val result = useCase.updateTask(1L, "New", "New desc", null, false, null, null)

        assertTrue(result.isSuccess)
        assertEquals("New", repository.getTaskById(1)!!.title)
    }

    @Test
    fun `updateTask with nonexistent id fails`() = runTest {
        val result = useCase.updateTask(999L, "Title", "", null, false, null, null)
        assertTrue(result.isFailure)
    }

    // === deleteTask ===

    @Test
    fun `deleteTask removes task`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = useCase.deleteTask(task)

        assertTrue(result.isSuccess)
        assertNull(repository.getTaskById(1))
    }

    // === toggleTaskCompletion ===

    @Test
    fun `toggleTaskCompletion flips completion state`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = useCase.toggleTaskCompletion(task)

        assertTrue(result.isSuccess)
        assertTrue(repository.getTaskById(1)!!.isCompleted)
    }

    // === restoreTask ===

    @Test
    fun `restoreTask upserts task`() = runTest {
        val task = TestTaskFactory.createTask(id = 5, title = "Restore me")

        val result = useCase.restoreTask(task)

        assertTrue(result.isSuccess)
        assertNotNull(repository.getTaskById(5))
    }

    // === restoreTasks ===

    @Test
    fun `restoreTasks upserts multiple tasks`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "A"),
            TestTaskFactory.createTask(id = 2, title = "B"),
        )

        useCase.restoreTasks(tasks)

        assertNotNull(repository.getTaskById(1))
        assertNotNull(repository.getTaskById(2))
    }

    // === bulk operations ===

    @Test
    fun `bulkSetCompleted marks tasks as completed`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        useCase.bulkSetCompleted(listOf(1L, 2L), true)

        assertTrue(repository.getTaskById(1)!!.isCompleted)
        assertTrue(repository.getTaskById(2)!!.isCompleted)
        assertFalse(repository.getTaskById(3)!!.isCompleted)
    }

    @Test
    fun `bulkSetCompleted marks tasks as active`() = runTest {
        val tasks = listOf(
            TestTaskFactory.completedTask(id = 1),
            TestTaskFactory.completedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        useCase.bulkSetCompleted(listOf(1L, 2L), false)

        assertFalse(repository.getTaskById(1)!!.isCompleted)
        assertFalse(repository.getTaskById(2)!!.isCompleted)
    }

    @Test
    fun `bulkDeleteTasks removes tasks`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        useCase.bulkDeleteTasks(listOf(1L, 2L))

        assertNull(repository.getTaskById(1))
        assertNull(repository.getTaskById(2))
        assertNotNull(repository.getTaskById(3))
    }

    // === pin and priority ===

    @Test
    fun `setPinned updates pin status`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = useCase.setPinned(1L, true)

        assertTrue(result.isSuccess)
        assertTrue(repository.getTaskById(1)!!.isPinned)
    }

    @Test
    fun `setPriority updates priority`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, priority = 0)
        repository.seed(task)

        val result = useCase.setPriority(1L, 2)

        assertTrue(result.isSuccess)
        assertEquals(2, repository.getTaskById(1)!!.priority)
    }

    // === archive operations ===

    @Test
    fun `archiveTask sets archived flag`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        val result = useCase.archiveTask(1L)

        assertTrue(result.isSuccess)
        assertTrue(repository.getTaskById(1)!!.isArchived)
    }

    @Test
    fun `unarchiveTask clears archived flag`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 1)
        repository.seed(task)

        val result = useCase.unarchiveTask(1L)

        assertTrue(result.isSuccess)
        assertFalse(repository.getTaskById(1)!!.isArchived)
    }

    @Test
    fun `hardDeleteTask removes task permanently`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 1)
        repository.seed(task)

        val result = useCase.hardDeleteTask(1L)

        assertTrue(result.isSuccess)
        assertNull(repository.getTaskById(1))
    }

    @Test
    fun `bulkArchiveTasks archives multiple tasks`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        useCase.bulkArchiveTasks(listOf(1L, 2L))

        assertTrue(repository.getTaskById(1)!!.isArchived)
        assertTrue(repository.getTaskById(2)!!.isArchived)
        assertFalse(repository.getTaskById(3)!!.isArchived)
    }

    @Test
    fun `bulkUnarchiveTasks unarchives multiple tasks`() = runTest {
        val tasks = listOf(
            TestTaskFactory.archivedTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        useCase.bulkUnarchiveTasks(listOf(1L, 2L))

        assertFalse(repository.getTaskById(1)!!.isArchived)
        assertFalse(repository.getTaskById(2)!!.isArchived)
    }

    @Test
    fun `bulkHardDeleteTasks removes permanently`() = runTest {
        val tasks = listOf(
            TestTaskFactory.archivedTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        useCase.bulkHardDeleteTasks(listOf(1L, 2L))

        assertNull(repository.getTaskById(1))
        assertNull(repository.getTaskById(2))
    }

    // === observing tasks ===

    @Test
    fun `getArchivedTasks returns only archived`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        val result = useCase.getArchivedTasks().first()

        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    // === error state ===

    @Test
    fun `errorMessage is null initially`() {
        assertNull(useCase.errorMessage.value)
    }

    @Test
    fun `clearError resets error to null`() {
        useCase.clearError()
        assertNull(useCase.errorMessage.value)
    }
}
