package dev.tuandoan.tasktracker.work

import dev.tuandoan.tasktracker.domain.TaskManager
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the task completion flow triggered by notification "Mark Complete" action.
 * Exercises the same code path as [TaskCompleteReceiver]: getTaskById → toggleTaskCompletion.
 */
class NotificationCompleteFlowTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var taskManager: TaskManager

    @Before
    fun setup() {
        repository = FakeTaskRepository()
        scheduler = FakeReminderScheduler()
        taskManager = TaskManager(repository, scheduler)
    }

    @Test
    fun `completing a simple task marks it done`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Buy groceries")
        repository.seed(task)

        // Simulate the receiver flow: lookup + toggle
        val found = taskManager.getTaskById(1L)
        assertNotNull(found)
        taskManager.toggleTaskCompletion(found!!)

        val result = repository.getTaskById(1L)
        assertTrue(result!!.isCompleted)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `completing a task with reminder cancels the reminder`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Meeting",
            dueAt = System.currentTimeMillis() + 3600_000,
            reminderOffsetMinutes = 15,
        )
        repository.seed(task)

        val found = taskManager.getTaskById(1L)!!
        taskManager.toggleTaskCompletion(found)

        assertTrue(scheduler.cancelledTaskIds.contains(1L))
    }

    @Test
    fun `completing a recurring task generates next occurrence`() = runTest {
        val dueAt = System.currentTimeMillis() + 86400_000L
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Daily standup",
            dueAt = dueAt,
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        val found = taskManager.getTaskById(1L)!!
        taskManager.toggleTaskCompletion(found)

        // Original task should be completed
        val original = repository.getTaskById(1L)!!
        assertTrue(original.isCompleted)

        // A new task should be generated
        val allTasks = repository.getAllTasksSnapshot()
        assertEquals(2, allTasks.size)
        val next = allTasks.find { it.id != 1L }
        assertNotNull(next)
        assertEquals("Daily standup", next!!.title)
        assertEquals(false, next.isCompleted)
        assertNull(next.completedAt)
        assertEquals(1L, next.parentRecurringTaskId)
    }

    @Test
    fun `completing a recurring task with reminder schedules reminder for next occurrence`() = runTest {
        val dueAt = System.currentTimeMillis() + 86400_000L
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Daily standup",
            dueAt = dueAt,
            reminderOffsetMinutes = 15,
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        val found = taskManager.getTaskById(1L)!!
        taskManager.toggleTaskCompletion(found)

        // Old reminder cancelled
        assertTrue(scheduler.cancelledTaskIds.contains(1L))

        // New reminder scheduled for the generated task
        val allTasks = repository.getAllTasksSnapshot()
        val next = allTasks.find { it.id != 1L }!!
        assertTrue(scheduler.scheduledReminders.any { it.taskId == next.id })
    }

    @Test
    fun `completing an already completed task does nothing`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Already done",
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
        )
        repository.seed(task)

        // Simulate receiver guard: only toggle if not completed
        val found = taskManager.getTaskById(1L)!!
        if (!found.isCompleted) {
            taskManager.toggleTaskCompletion(found)
        }

        // Task should remain completed (no change)
        assertTrue(repository.getTaskById(1L)!!.isCompleted)
    }

    @Test
    fun `non-existent task id returns null and does nothing`() = runTest {
        val found = taskManager.getTaskById(999L)
        assertNull(found)
        // No crash, no side effects
    }

    @Test
    fun `completing a weekly recurring task generates next with correct parent chain`() = runTest {
        val dueAt = System.currentTimeMillis() + 86400_000L
        val task = TestTaskFactory.createTask(
            id = 1,
            title = "Weekly review",
            dueAt = dueAt,
            recurrenceType = RecurrenceType.WEEKLY.value,
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = 4, // Wednesday
        )
        repository.seed(task)

        val found = taskManager.getTaskById(1L)!!
        taskManager.toggleTaskCompletion(found)

        val allTasks = repository.getAllTasksSnapshot()
        val next = allTasks.find { it.id != 1L }
        assertNotNull(next)
        // parentRecurringTaskId should point to the original task
        assertEquals(1L, next!!.parentRecurringTaskId)
        assertEquals(RecurrenceType.WEEKLY.value, next.recurrenceType)
    }
}
