package dev.tuandoan.tasktracker.domain.usecase

import android.content.Context
import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskCrudUseCaseDuplicateTest {

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
        taskManager = TaskManager(repository, scheduler)
        useCase = TaskCrudUseCase(taskManager, context)
    }

    @Test
    fun `duplicateTask copies title from source`() = runTest {
        val source = TestTaskFactory.createTask(id = 1, title = "Original Title")
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        assertTrue(result.isSuccess)
        val newId = result.getOrThrow()
        val duplicate = repository.getTaskById(newId)
        assertNotNull(duplicate)
        assertEquals("Original Title", duplicate!!.title)
    }

    @Test
    fun `duplicateTask copies description from source`() = runTest {
        val source = TestTaskFactory.createTask(id = 1, description = "Original Desc")
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertEquals("Original Desc", duplicate!!.description)
    }

    @Test
    fun `duplicateTask copies tag from source`() = runTest {
        val source = TestTaskFactory.createTask(id = 1, tag = "work")
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertEquals("work", duplicate!!.tag)
    }

    @Test
    fun `duplicateTask copies priority from source`() = runTest {
        val source = TestTaskFactory.createTask(id = 1, priority = 2) // HIGH
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertEquals(2, duplicate!!.priority)
    }

    @Test
    fun `duplicateTask resets isCompleted to false`() = runTest {
        val source = TestTaskFactory.completedTask(id = 1)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertFalse(duplicate!!.isCompleted)
    }

    @Test
    fun `duplicateTask resets completedAt to null`() = runTest {
        val source = TestTaskFactory.completedTask(id = 1)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertNull(duplicate!!.completedAt)
    }

    @Test
    fun `duplicateTask resets dueAt to null`() = runTest {
        val source = TestTaskFactory.taskWithDueDate(id = 1)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertNull(duplicate!!.dueAt)
    }

    @Test
    fun `duplicateTask resets reminderOffsetMinutes to null`() = runTest {
        val source = TestTaskFactory.taskWithDueDate(id = 1, reminderOffsetMinutes = 60)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertNull(duplicate!!.reminderOffsetMinutes)
    }

    @Test
    fun `duplicateTask resets isPinned to false`() = runTest {
        val source = TestTaskFactory.pinnedTask(id = 1)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertFalse(duplicate!!.isPinned)
    }

    @Test
    fun `duplicateTask resets isArchived to false`() = runTest {
        val source = TestTaskFactory.archivedTask(id = 1)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val duplicate = repository.getTaskById(result.getOrThrow())
        assertFalse(duplicate!!.isArchived)
    }

    @Test
    fun `duplicateTask assigns new id different from source`() = runTest {
        val source = TestTaskFactory.createTask(id = 1)
        repository.seed(source)

        val result = useCase.duplicateTask(source)

        val newId = result.getOrThrow()
        assertNotEquals(source.id, newId)
    }

    @Test
    fun `duplicateTask creates exactly one new task`() = runTest {
        val source = TestTaskFactory.createTask(id = 1)
        repository.seed(source)

        val initialCount = repository.getAllTasksSnapshot().size

        useCase.duplicateTask(source)

        assertEquals(initialCount + 1, repository.getAllTasksSnapshot().size)
    }
}
