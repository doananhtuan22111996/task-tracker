package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent
import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.diagnostics.AnalyticsPriority
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskManagerTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var subtaskRepository: FakeSubtaskRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var widgetUpdater: FakeWidgetUpdater
    private lateinit var breadcrumbLogger: BreadcrumbLogger
    private lateinit var analyticsLogger: AnalyticsLogger
    private lateinit var manager: TaskManager

    @Before
    fun setup() {
        repository = FakeTaskRepository()
        subtaskRepository = FakeSubtaskRepository()
        scheduler = FakeReminderScheduler()
        widgetUpdater = FakeWidgetUpdater()
        breadcrumbLogger = mockk(relaxed = true)
        analyticsLogger = mockk(relaxed = true)
        manager =
            TaskManager(repository, subtaskRepository, scheduler, widgetUpdater, breadcrumbLogger, analyticsLogger)
    }

    // === createTask ===

    @Test
    fun `createTask with valid title inserts and returns id`() = runTest {
        val id = manager.createTask("Buy milk", "From the store")

        assertTrue(id > 0)
        val task = repository.getTaskById(id)
        assertNotNull(task)
        assertEquals("Buy milk", task!!.title)
        assertEquals("From the store", task.description)
    }

    @Test
    fun `createTask trims whitespace from title and description`() = runTest {
        val id = manager.createTask("  Trim me  ", "  Desc  ")

        val task = repository.getTaskById(id)
        assertEquals("Trim me", task!!.title)
        assertEquals("Desc", task.description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createTask with blank title throws`() = runTest {
        manager.createTask("   ", "")
    }

    @Test
    fun `createTask with due date and reminder schedules reminder`() = runTest {
        val dueAt = TestTaskFactory.BASE_TIMESTAMP + TestTaskFactory.ONE_DAY_MS
        manager.createTask("Task", "", dueAt, 60)

        assertEquals(1, scheduler.scheduledReminders.size)
        assertEquals(60, scheduler.scheduledReminders[0].offsetMinutes)
    }

    @Test
    fun `createTask with null reminder does not schedule`() = runTest {
        manager.createTask("Task", "", null, null)
        assertTrue(scheduler.scheduledReminders.isEmpty())
    }

    @Test
    fun `createTask with zero reminder offset does not schedule`() = runTest {
        val dueAt = TestTaskFactory.BASE_TIMESTAMP + TestTaskFactory.ONE_DAY_MS
        manager.createTask("Task", "", dueAt, 0)
        assertTrue(scheduler.scheduledReminders.isEmpty())
    }

    @Test
    fun `createTask normalizes tag to uppercase and trims whitespace`() = runTest {
        val id = manager.createTask("Task", "", null, false, null, "  work  ")
        val task = repository.getTaskById(id)
        assertEquals("WORK", task!!.tag)
    }

    @Test
    fun `createTask with blank tag stores null`() = runTest {
        val id = manager.createTask("Task", "", null, false, null, "   ")
        val task = repository.getTaskById(id)
        assertNull(task!!.tag)
    }

    @Test
    fun `createTask uppercases Vietnamese tag via Locale ROOT`() = runTest {
        val id = manager.createTask("Task", "", null, false, null, "việc")
        val task = repository.getTaskById(id)
        assertEquals("VIỆC", task!!.tag)
    }

    @Test
    fun `updateTask normalizes tag on whole-task write`() = runTest {
        val original = TestTaskFactory.createTask(id = 1, title = "T").copy(tag = "WORK")
        repository.seed(original)

        manager.updateTask(original.copy(tag = "  home  "))

        val updated = repository.getTaskById(1)!!
        assertEquals("HOME", updated.tag)
    }

    @Test
    fun `updateTask collapses blank tag to null`() = runTest {
        val original = TestTaskFactory.createTask(id = 1).copy(tag = "WORK")
        repository.seed(original)

        manager.updateTask(original.copy(tag = "   "))

        val updated = repository.getTaskById(1)!!
        assertNull(updated.tag)
    }

    @Test
    fun `updateTaskContent normalizes tag to uppercase`() = runTest {
        val original = TestTaskFactory.createTask(id = 1, title = "Old")
        repository.seed(original)

        manager.updateTaskContent(1, "New", "desc", null, false, null, "  personal  ")

        val updated = repository.getTaskById(1)!!
        assertEquals("PERSONAL", updated.tag)
    }

    @Test
    fun `updateTask clears tagColor when tag becomes null`() = runTest {
        val original = TestTaskFactory.createTask(id = 1).copy(tag = "WORK", tagColor = "#FF0000")
        repository.seed(original)

        // Caller-supplied tagColor is dropped because the normalized tag is null.
        manager.updateTask(original.copy(tag = "  "))

        val updated = repository.getTaskById(1)!!
        assertNull(updated.tag)
        assertNull(updated.tagColor)
    }

    @Test
    fun `updateTaskContent clears tagColor when tag becomes null`() = runTest {
        val original = TestTaskFactory.createTask(id = 1, title = "Old").copy(tag = "WORK", tagColor = "#FF0000")
        repository.seed(original)

        manager.updateTaskContent(1, "Old", "", null, false, null, null)

        val updated = repository.getTaskById(1)!!
        assertNull(updated.tag)
        assertNull(updated.tagColor)
    }

    // === toggleTaskCompletion ===

    @Test
    fun `toggleTaskCompletion marks active task as completed with timestamp`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Active")
        repository.seed(task)

        manager.toggleTaskCompletion(task)

        val updated = repository.getTaskById(1)!!
        assertTrue(updated.isCompleted)
        assertNotNull(updated.completedAt)
    }

    @Test
    fun `toggleTaskCompletion marks completed task as active and clears completedAt`() = runTest {
        val task = TestTaskFactory.completedTask(id = 1)
        repository.seed(task)

        manager.toggleTaskCompletion(task)

        val updated = repository.getTaskById(1)!!
        assertFalse(updated.isCompleted)
        assertNull(updated.completedAt)
    }

    @Test
    fun `toggleTaskCompletion cancels reminder when completing`() = runTest {
        val task = TestTaskFactory.createTask(id = 5, title = "Active")
        repository.seed(task)

        manager.toggleTaskCompletion(task)

        assertTrue(scheduler.cancelledTaskIds.contains(5L))
    }

    @Test
    fun `toggleTaskCompletion reschedules reminder when uncompleting with due date`() = runTest {
        val task = TestTaskFactory.completedTask(id = 5).copy(
            dueAt = TestTaskFactory.BASE_TIMESTAMP + TestTaskFactory.ONE_DAY_MS,
            reminderOffsetMinutes = 60,
        )
        repository.seed(task)

        manager.toggleTaskCompletion(task)

        assertEquals(1, scheduler.scheduledReminders.size)
    }

    // === markTaskComplete / markTaskIncomplete ===

    @Test
    fun `markTaskComplete is idempotent for already completed task`() = runTest {
        val task = TestTaskFactory.completedTask(id = 1, completedAt = 5000L)
        repository.seed(task)

        manager.markTaskComplete(task)

        // Should not have changed the completedAt
        val updated = repository.getTaskById(1)!!
        assertEquals(5000L, updated.completedAt)
    }

    @Test
    fun `markTaskIncomplete is idempotent for already active task`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        manager.markTaskIncomplete(task)

        val updated = repository.getTaskById(1)!!
        assertFalse(updated.isCompleted)
    }

    // === deleteTask ===

    @Test
    fun `deleteTask removes task and cancels reminder`() = runTest {
        val task = TestTaskFactory.createTask(id = 3)
        repository.seed(task)

        manager.deleteTask(task)

        assertNull(repository.getTaskById(3))
        assertTrue(scheduler.cancelledTaskIds.contains(3L))
    }

    // === restoreTask ===

    @Test
    fun `restoreTask upserts task back`() = runTest {
        val task = TestTaskFactory.createTask(id = 7, title = "Restore me")

        val result = manager.restoreTask(task)

        assertTrue(result.isSuccess)
        assertNotNull(repository.getTaskById(7))
    }

    // === bulk operations ===

    @Test
    fun `setCompletedBulk with empty ids does nothing`() = runTest {
        manager.setCompletedBulk(emptyList(), true)
        assertTrue(scheduler.cancelledTaskIds.isEmpty())
    }

    @Test
    fun `setCompletedBulk marks multiple tasks completed and cancels reminders`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        manager.setCompletedBulk(listOf(1L, 2L, 3L), true)

        for (id in 1L..3L) {
            assertTrue(scheduler.cancelledTaskIds.contains(id))
        }
    }

    @Test
    fun `setCompletedBulk with completed=false marks active and reschedules`() = runTest {
        val tasks = listOf(
            TestTaskFactory.completedTask(id = 1).copy(
                dueAt = TestTaskFactory.BASE_TIMESTAMP + TestTaskFactory.ONE_DAY_MS,
                reminderOffsetMinutes = 5,
            ),
        )
        repository.seed(*tasks.toTypedArray())

        manager.setCompletedBulk(listOf(1L), false)

        // Should have rescheduled reminder
        assertEquals(1, scheduler.scheduledReminders.size)
    }

    @Test
    fun `deleteTasksByIds with empty ids does nothing`() = runTest {
        manager.deleteTasksByIds(emptyList())
        assertTrue(scheduler.cancelledTaskIds.isEmpty())
    }

    @Test
    fun `deleteTasksByIds removes tasks and cancels reminders`() = runTest {
        val tasks = TestTaskFactory.createTaskList(3)
        repository.seed(*tasks.toTypedArray())

        manager.deleteTasksByIds(listOf(1L, 2L))

        assertNull(repository.getTaskById(1))
        assertNull(repository.getTaskById(2))
        assertNotNull(repository.getTaskById(3))
    }

    // === archive operations ===

    @Test
    fun `archiveTask sets archived flag and cancels reminder`() = runTest {
        val task = TestTaskFactory.createTask(id = 10)
        repository.seed(task)

        manager.archiveTask(10)

        val archived = repository.getTaskById(10)!!
        assertTrue(archived.isArchived)
        assertNotNull(archived.archivedAt)
        assertTrue(scheduler.cancelledTaskIds.contains(10L))
    }

    @Test
    fun `unarchiveTask clears archived flag and reschedules if needed`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 10).copy(
            dueAt = TestTaskFactory.BASE_TIMESTAMP + TestTaskFactory.ONE_DAY_MS,
            reminderOffsetMinutes = 60,
        )
        repository.seed(task)

        manager.unarchiveTask(10)

        val unarchived = repository.getTaskById(10)!!
        assertFalse(unarchived.isArchived)
        assertNull(unarchived.archivedAt)
        assertEquals(1, scheduler.scheduledReminders.size)
    }

    @Test
    fun `hardDeleteTask removes permanently and cancels reminder`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 20)
        repository.seed(task)

        manager.hardDeleteTask(20)

        assertNull(repository.getTaskById(20))
        assertTrue(scheduler.cancelledTaskIds.contains(20L))
    }

    @Test
    fun `hardDeleteTasks with empty ids does nothing`() = runTest {
        manager.hardDeleteTasks(emptyList())
        assertTrue(scheduler.cancelledTaskIds.isEmpty())
    }

    // === updateTaskContent ===

    @Test
    fun `updateTaskContent updates title and description`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Old", description = "Old desc")
        repository.seed(task)

        manager.updateTaskContent(1, "New", "New desc")

        val updated = repository.getTaskById(1)!!
        assertEquals("New", updated.title)
        assertEquals("New desc", updated.description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateTaskContent with blank title throws`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)

        manager.updateTaskContent(1, "  ", "desc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateTaskContent with nonexistent id throws`() = runTest {
        manager.updateTaskContent(999, "Title", "desc")
    }

    // === data access ===

    @Test
    fun `getAllTasks excludes archived tasks`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Active"),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        val result = manager.getAllTasks().first()

        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun `getArchivedTasks returns only archived`() = runTest {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1),
            TestTaskFactory.archivedTask(id = 2),
        )
        repository.seed(*tasks.toTypedArray())

        val result = manager.getArchivedTasks().first()

        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    // === scheduleReminderIfNeeded error ===

    @Test(expected = IllegalArgumentException::class)
    fun `createTask throws when reminder scheduling fails`() = runTest {
        scheduler.shouldScheduleSucceed = false
        val dueAt = TestTaskFactory.BASE_TIMESTAMP + TestTaskFactory.ONE_DAY_MS
        manager.createTask("Task", "", dueAt, 60)
    }

    // === Widget update triggers ===

    @Test
    fun `createTask triggers widget update`() = runTest {
        manager.createTask("Task", "")
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `updateTask triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.updateTask(task.copy(title = "Updated"))
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `updateTaskContent triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Original")
        repository.seed(task)
        manager.updateTaskContent(task.id, "Updated", "desc")
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `deleteTask triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.deleteTask(task)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `restoreTask triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        manager.restoreTask(task)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `toggleTaskCompletion triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.toggleTaskCompletion(task)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `setCompletedBulk triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.setCompletedBulk(listOf(1L), true)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `deleteTasksByIds triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.deleteTasksByIds(listOf(1L))
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `setPinned triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.setPinned(1L, true)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `setPriority triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.setPriority(1L, 2)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `archiveTask triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.archiveTask(1L)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `unarchiveTask triggers widget update`() = runTest {
        val task = TestTaskFactory.archivedTask(id = 1)
        repository.seed(task)
        manager.unarchiveTask(1L)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `hardDeleteTask triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.hardDeleteTask(1L)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `hardDeleteTasks triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.hardDeleteTasks(listOf(1L))
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `restoreTasks triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        manager.restoreTasks(listOf(task))
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `markTaskComplete triggers widget update`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.markTaskComplete(task)
        assertEquals(1, widgetUpdater.updateCount)
    }

    @Test
    fun `markTaskIncomplete triggers widget update`() = runTest {
        val task = TestTaskFactory.completedTask(id = 1)
        repository.seed(task)
        manager.markTaskIncomplete(task)
        assertEquals(1, widgetUpdater.updateCount)
    }

    // === FB-12: breadcrumb PII-safety contracts ===

    @Test
    fun `createTask emits TASK_ACTION breadcrumb with flags only and no title or tag`() = runTest {
        manager.createTask(
            title = "Secret title should not leak",
            description = "Sensitive notes",
            dueAt = 1_700_000_000_000L,
            dueAtHasTime = true,
            reminderOffsetMinutes = 30,
            tag = "work",
            recurrenceType = 0,
        )
        verify {
            breadcrumbLogger.log(
                BreadcrumbCategory.TASK_ACTION,
                "create hasDue=true hasReminder=true recurrence=0",
            )
        }
    }

    @Test
    fun `updateTask emits TASK_ACTION breadcrumb with id and change flags only`() = runTest {
        val original = TestTaskFactory.createTask(id = 42, title = "Top secret")
        repository.seed(original)
        val edited = original.copy(
            title = "Still secret",
            dueAt = 1_700_000_000_000L,
            reminderOffsetMinutes = 15,
        )
        manager.updateTask(edited)
        verify {
            breadcrumbLogger.log(
                BreadcrumbCategory.TASK_ACTION,
                "update id=42 dueChanged=true reminderChanged=true",
            )
        }
    }

    @Test
    fun `archiveTask emits TASK_ACTION breadcrumb carrying id only`() = runTest {
        val task = TestTaskFactory.createTask(id = 7, title = "Private")
        repository.seed(task)
        manager.archiveTask(7L)
        verify { breadcrumbLogger.log(BreadcrumbCategory.TASK_ACTION, "archive id=7") }
    }

    // === FB-14: AnalyticsEvent emissions with PII-safety contracts ===

    @Test
    fun `createTask emits TaskCreated event with flags and clamped priority only`() = runTest {
        manager.createTask(
            title = "Secret title should not leak",
            description = "Sensitive notes",
            dueAt = 1_700_000_000_000L,
            dueAtHasTime = true,
            reminderOffsetMinutes = 30,
            tag = "work",
            recurrenceType = 0,
        )
        verify {
            analyticsLogger.log(
                AnalyticsEvent.TaskCreated(
                    hasDueDate = true,
                    hasTag = true,
                    priority = AnalyticsPriority.MEDIUM,
                    isRecurring = false,
                ),
            )
        }
    }

    @Test
    fun `archiveTask emits TaskArchived event`() = runTest {
        val task = TestTaskFactory.createTask(id = 7, title = "Private")
        repository.seed(task)
        manager.archiveTask(7L)
        verify { analyticsLogger.log(AnalyticsEvent.TaskArchived) }
    }

    @Test
    fun `toggleTaskCompletion emits TaskCompleted on complete direction only`() = runTest {
        val task = TestTaskFactory.createTask(id = 1, title = "Confidential")
        repository.seed(task)
        manager.toggleTaskCompletion(task)
        verify(exactly = 1) { analyticsLogger.log(AnalyticsEvent.TaskCompleted) }
    }

    @Test
    fun `toggleTaskCompletion does NOT emit TaskCompleted on un-complete direction`() = runTest {
        val completed = TestTaskFactory.completedTask(id = 1)
        repository.seed(completed)
        manager.toggleTaskCompletion(completed)
        // Negative invariant: un-complete is a correction; no matching event in FR-15.
        verify(exactly = 0) { analyticsLogger.log(AnalyticsEvent.TaskCompleted) }
    }

    @Test
    fun `markTaskComplete emits TaskCompleted exactly once and is idempotent`() = runTest {
        val task = TestTaskFactory.createTask(id = 1)
        repository.seed(task)
        manager.markTaskComplete(task)
        // Second call on an already-completed task must NOT re-fire the event.
        val completed = task.copy(isCompleted = true)
        manager.markTaskComplete(completed)
        verify(exactly = 1) { analyticsLogger.log(AnalyticsEvent.TaskCompleted) }
    }

    @Test
    fun `unarchiveTask emits TaskRestored event`() = runTest {
        val archived = TestTaskFactory.createTask(id = 11).copy(isArchived = true)
        repository.seed(archived)
        manager.unarchiveTask(11L)
        verify { analyticsLogger.log(AnalyticsEvent.TaskRestored) }
    }
}
