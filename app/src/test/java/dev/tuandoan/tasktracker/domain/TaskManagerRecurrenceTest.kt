package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import dev.tuandoan.tasktracker.testutil.FakeReminderScheduler
import dev.tuandoan.tasktracker.testutil.FakeSubtaskRepository
import dev.tuandoan.tasktracker.testutil.FakeTaskRepository
import dev.tuandoan.tasktracker.testutil.FakeWidgetUpdater
import dev.tuandoan.tasktracker.testutil.TestSubtaskFactory
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskManagerRecurrenceTest {

    private lateinit var repository: FakeTaskRepository
    private lateinit var subtaskRepository: FakeSubtaskRepository
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var taskManager: TaskManager

    private val zone = ZoneId.systemDefault()

    private fun dateToEpoch(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        subtaskRepository = FakeSubtaskRepository()
        reminderScheduler = FakeReminderScheduler()
        taskManager = TaskManager(repository, subtaskRepository, reminderScheduler, FakeWidgetUpdater())
    }

    // ── Complete generates next occurrence ──

    @Test
    fun `completing daily recurring task generates next occurrence`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Daily standup",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val allTasks = repository.getAllTasksSnapshot()
        assertEquals(2, allTasks.size)

        val original = allTasks.find { it.id == 1L }!!
        assertTrue(original.isCompleted)
        assertNotNull(original.completedAt)

        val generated = allTasks.find { it.id != 1L }!!
        assertFalse(generated.isCompleted)
        assertNull(generated.completedAt)
        assertEquals(dateToEpoch(2026, 3, 28), generated.dueAt)
        assertEquals("Daily standup", generated.title)
        assertEquals(RecurrenceType.DAILY.value, generated.recurrenceType)
        assertEquals(1, generated.recurrenceInterval)
        assertEquals(1L, generated.parentRecurringTaskId)
    }

    @Test
    fun `completing recurring task preserves all fields on generated instance`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Weekly review",
            description = "Review sprint",
            dueAt = dateToEpoch(2026, 3, 27),
            tag = "work",
            priority = 2,
            reminderOffsetMinutes = 60,
            recurrenceType = RecurrenceType.WEEKLY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val generated = repository.getAllTasksSnapshot().find { it.id != 1L }!!
        assertEquals("Weekly review", generated.title)
        assertEquals("Review sprint", generated.description)
        assertEquals("work", generated.tag)
        assertEquals(2, generated.priority)
        assertEquals(60, generated.reminderOffsetMinutes)
        assertEquals(RecurrenceType.WEEKLY.value, generated.recurrenceType)
        assertEquals(1, generated.recurrenceInterval)
    }

    @Test
    fun `completing recurring task chains parentRecurringTaskId correctly`() = runTest {
        // First task in chain (no parent)
        val task = TestTaskFactory.createTask(
            id = 10L,
            title = "Recurring",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            parentRecurringTaskId = null,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val generated = repository.getAllTasksSnapshot().find { it.id != 10L }!!
        assertEquals(10L, generated.parentRecurringTaskId) // Points to first task
    }

    @Test
    fun `completing chained recurring task preserves parentRecurringTaskId`() = runTest {
        // This is a generated instance (has parent = 10)
        val task = TestTaskFactory.createTask(
            id = 20L,
            title = "Recurring",
            dueAt = dateToEpoch(2026, 3, 28),
            recurrenceType = RecurrenceType.DAILY.value,
            parentRecurringTaskId = 10L,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val generated = repository.getAllTasksSnapshot().find { it.id != 20L }!!
        assertEquals(10L, generated.parentRecurringTaskId) // Still points to original
    }

    @Test
    fun `completing non-recurring task does not generate new task`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "One-off task",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.NONE.value,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        assertEquals(1, repository.getAllTasksSnapshot().size)
    }

    @Test
    fun `completing recurring task with end date reached does not generate`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Limited recurring",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceEndDate = dateToEpoch(2026, 3, 27), // End date = current due
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        assertEquals(1, repository.getAllTasksSnapshot().size)
        assertTrue(repository.getAllTasksSnapshot().first().isCompleted)
    }

    // ── Un-complete deletes generated instance ──

    @Test
    fun `un-completing recurring task deletes generated next instance`() = runTest {
        val original = TestTaskFactory.createTask(
            id = 1L,
            title = "Daily task",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
        )
        val generated = TestTaskFactory.createTask(
            id = 2L,
            title = "Daily task",
            dueAt = dateToEpoch(2026, 3, 28),
            recurrenceType = RecurrenceType.DAILY.value,
            parentRecurringTaskId = 1L,
        )
        repository.seed(original, generated)

        taskManager.toggleTaskCompletion(original)

        val remaining = repository.getAllTasksSnapshot()
        assertEquals(1, remaining.size)
        assertEquals(1L, remaining.first().id)
        assertFalse(remaining.first().isCompleted)
    }

    @Test
    fun `un-completing non-recurring task does not delete anything`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Regular task",
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            recurrenceType = RecurrenceType.NONE.value,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        assertEquals(1, repository.getAllTasksSnapshot().size)
        assertFalse(repository.getAllTasksSnapshot().first().isCompleted)
    }

    // ── Skip occurrence ──

    @Test
    fun `skip archives current and generates next occurrence`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Weekly review",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.WEEKLY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        taskManager.skipOccurrence(task)

        val allTasks = repository.getAllTasksSnapshot()
        assertEquals(2, allTasks.size)

        val skipped = allTasks.find { it.id == 1L }!!
        assertTrue(skipped.isArchived)
        assertFalse(skipped.isCompleted)

        val generated = allTasks.find { it.id != 1L }!!
        assertFalse(generated.isArchived)
        assertFalse(generated.isCompleted)
        assertEquals(dateToEpoch(2026, 4, 3), generated.dueAt)
        assertEquals(1L, generated.parentRecurringTaskId)
    }

    @Test
    fun `skip on non-recurring task does nothing`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "One-off",
            recurrenceType = RecurrenceType.NONE.value,
        )
        repository.seed(task)

        taskManager.skipOccurrence(task)

        val allTasks = repository.getAllTasksSnapshot()
        assertEquals(1, allTasks.size)
        assertFalse(allTasks.first().isArchived)
    }

    @Test
    fun `skip with end date reached archives but does not generate`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Limited",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceEndDate = dateToEpoch(2026, 3, 27),
        )
        repository.seed(task)

        taskManager.skipOccurrence(task)

        val allTasks = repository.getAllTasksSnapshot()
        assertEquals(1, allTasks.size)
        assertTrue(allTasks.first().isArchived)
    }

    // ── Reminder scheduling on generated tasks ──

    @Test
    fun `generated task gets reminder scheduled`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Reminder task",
            dueAt = dateToEpoch(2026, 12, 27),
            reminderOffsetMinutes = 30,
            recurrenceType = RecurrenceType.DAILY.value,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val generated = repository.getAllTasksSnapshot().find { it.id != 1L }!!
        // FakeReminderScheduler tracks scheduled tasks
        assertTrue(reminderScheduler.isScheduled(generated.id))
    }

    @Test
    fun `un-completing cancels reminder on deleted generated task`() = runTest {
        val original = TestTaskFactory.createTask(
            id = 1L,
            title = "Task",
            dueAt = dateToEpoch(2026, 3, 27),
            reminderOffsetMinutes = 30,
            recurrenceType = RecurrenceType.DAILY.value,
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
        )
        val generated = TestTaskFactory.createTask(
            id = 2L,
            title = "Task",
            dueAt = dateToEpoch(2026, 3, 28),
            reminderOffsetMinutes = 30,
            recurrenceType = RecurrenceType.DAILY.value,
            parentRecurringTaskId = 1L,
        )
        repository.seed(original, generated)
        reminderScheduler.schedule(2L, "Task", dateToEpoch(2026, 3, 28), 30)

        taskManager.toggleTaskCompletion(original)

        assertFalse(reminderScheduler.isScheduled(2L))
    }

    // ── Atomicity: complete + generate is single logical operation ──

    @Test
    fun `completing recurring task without due date does not generate`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "No due date recurring",
            dueAt = null,
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val allTasks = repository.getAllTasksSnapshot()
        assertEquals(1, allTasks.size)
        assertTrue(allTasks.first().isCompleted)
    }

    @Test
    fun `completing and un-completing recurring task returns to original state`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Round trip",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        // Complete → generates next
        taskManager.toggleTaskCompletion(task)
        assertEquals(2, repository.getAllTasksSnapshot().size)

        // Un-complete → deletes generated, restores original
        val completedTask = repository.getAllTasksSnapshot().find { it.id == 1L }!!
        taskManager.toggleTaskCompletion(completedTask)

        val remaining = repository.getAllTasksSnapshot()
        assertEquals(1, remaining.size)
        assertEquals(1L, remaining.first().id)
        assertFalse(remaining.first().isCompleted)
        assertNull(remaining.first().completedAt)
    }

    @Test
    fun `skip then complete on generated task creates correct chain`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Chain test",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
            recurrenceInterval = 1,
        )
        repository.seed(task)

        // Skip first occurrence
        taskManager.skipOccurrence(task)
        val afterSkip = repository.getAllTasksSnapshot()
        assertEquals(2, afterSkip.size)
        val skippedGen = afterSkip.find { it.id != 1L }!!
        assertEquals(dateToEpoch(2026, 3, 28), skippedGen.dueAt)
        assertEquals(1L, skippedGen.parentRecurringTaskId)

        // Complete the generated occurrence
        taskManager.toggleTaskCompletion(skippedGen)
        val afterComplete = repository.getAllTasksSnapshot()
        assertEquals(3, afterComplete.size)

        val finalGen = afterComplete.find { it.id != 1L && it.id != skippedGen.id }!!
        assertEquals(dateToEpoch(2026, 3, 29), finalGen.dueAt)
        assertEquals(1L, finalGen.parentRecurringTaskId) // Still points to original
    }

    // ── Subtask copy on recurrence ──

    @Test
    fun `completing recurring task with subtasks copies them reset to unchecked on new instance`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Weekly chores",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
        )
        repository.seed(task)
        subtaskRepository.seed(
            TestSubtaskFactory.createSubtask(id = 100L, taskId = 1L, title = "Milk", sortOrder = 0, isCompleted = true),
            TestSubtaskFactory.createSubtask(id = 101L, taskId = 1L, title = "Eggs", sortOrder = 1, isCompleted = true),
        )

        taskManager.toggleTaskCompletion(task)

        val generated = repository.getAllTasksSnapshot().single { it.id != 1L }
        val copied = subtaskRepository.getSubtasks(generated.id)
        assertEquals(listOf("Milk", "Eggs"), copied.map { it.title })
        assertEquals(listOf(0, 1), copied.map { it.sortOrder })
        assertTrue(copied.all { !it.isCompleted })

        // Original subtasks on the parent task are untouched.
        val originalSubtasks = subtaskRepository.getSubtasks(1L)
        assertEquals(2, originalSubtasks.size)
        assertTrue(originalSubtasks.all { it.isCompleted })
    }

    @Test
    fun `completing recurring task with no subtasks does not create any`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "No-checklist task",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
        )
        repository.seed(task)

        taskManager.toggleTaskCompletion(task)

        val generated = repository.getAllTasksSnapshot().single { it.id != 1L }
        assertEquals(0, subtaskRepository.countForTask(generated.id))
    }

    @Test
    fun `skip on recurring task with subtasks copies them reset to unchecked`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "Weekly chores",
            dueAt = dateToEpoch(2026, 3, 27),
            recurrenceType = RecurrenceType.DAILY.value,
        )
        repository.seed(task)
        subtaskRepository.seed(
            TestSubtaskFactory.createSubtask(id = 200L, taskId = 1L, title = "Mop", sortOrder = 0, isCompleted = false),
            TestSubtaskFactory.createSubtask(id = 201L, taskId = 1L, title = "Dust", sortOrder = 1, isCompleted = true),
        )

        taskManager.skipOccurrence(task)

        val generated = repository.getAllTasksSnapshot().single { !it.isArchived }
        val copied = subtaskRepository.getSubtasks(generated.id)
        assertEquals(listOf("Mop", "Dust"), copied.map { it.title })
        assertTrue(copied.all { !it.isCompleted })
    }

    @Test
    fun `completing non-recurring task does not touch subtasks`() = runTest {
        val task = TestTaskFactory.createTask(
            id = 1L,
            title = "One-off",
            recurrenceType = RecurrenceType.NONE.value,
        )
        repository.seed(task)
        subtaskRepository.seed(
            TestSubtaskFactory.createSubtask(id = 300L, taskId = 1L, title = "Step 1", isCompleted = true),
        )

        taskManager.toggleTaskCompletion(task)

        val allSubtasks = subtaskRepository.getAllSubtasksSnapshot()
        assertEquals(1, allSubtasks.size)
        assertEquals(1L, allSubtasks.first().taskId)
        assertTrue(allSubtasks.first().isCompleted)
    }

    // ── materializeProjectedOccurrence (CAL-23) ───────────────────────────────────────────

    @Test
    fun `materializeProjectedOccurrence inserts concrete row on projected date`() = runTest {
        val baseDate = LocalDate.of(2026, 5, 4)
        repository.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = baseDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val target = LocalDate.of(2026, 5, 7)

        val newId = taskManager.materializeProjectedOccurrence(parentId = 1L, date = target, zone = zone)

        assertNotNull(newId)
        val inserted = repository.getAllTasksSnapshot().single { it.id == newId }
        assertEquals(1L, inserted.parentRecurringTaskId)
        assertEquals(target.atStartOfDay(zone).toInstant().toEpochMilli(), inserted.dueAt)
        assertEquals("Standup", inserted.title)
        assertFalse(inserted.isCompleted)
        assertFalse(inserted.isArchived)
    }

    @Test
    fun `materializeProjectedOccurrence is idempotent when chain already has row on date`() = runTest {
        val baseDate = LocalDate.of(2026, 5, 4)
        val existingDate = LocalDate.of(2026, 5, 11)
        repository.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = baseDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
            TestTaskFactory.createTask(
                id = 42L,
                title = "Standup",
                dueAt = existingDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
                parentRecurringTaskId = 1L,
            ),
        )
        val countBefore = repository.getAllTasksSnapshot().size

        val returnedId = taskManager.materializeProjectedOccurrence(parentId = 1L, date = existingDate, zone = zone)

        assertEquals(42L, returnedId)
        assertEquals(countBefore, repository.getAllTasksSnapshot().size) // no duplicate insert
    }

    @Test
    fun `materializeProjectedOccurrence called twice produces only one row`() = runTest {
        val baseDate = LocalDate.of(2026, 5, 4)
        repository.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = baseDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val target = LocalDate.of(2026, 5, 5)

        val first = taskManager.materializeProjectedOccurrence(parentId = 1L, date = target, zone = zone)
        val second = taskManager.materializeProjectedOccurrence(parentId = 1L, date = target, zone = zone)

        assertEquals(first, second)
        val chainCount = repository.getAllTasksSnapshot().count { it.id == 1L || it.parentRecurringTaskId == 1L }
        assertEquals(2, chainCount) // root + exactly one materialized child
    }

    @Test
    fun `materializeProjectedOccurrence preserves time-of-day when parent has dueAtHasTime`() = runTest {
        val baseDate = LocalDate.of(2026, 5, 4)
        val parentDueAt = baseDate.atTime(9, 30).atZone(zone).toInstant().toEpochMilli()
        repository.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = parentDueAt,
                dueAtHasTime = true,
                recurrenceType = RecurrenceType.DAILY.value,
            ),
        )
        val target = LocalDate.of(2026, 5, 7)

        val newId = taskManager.materializeProjectedOccurrence(parentId = 1L, date = target, zone = zone)

        val inserted = repository.getAllTasksSnapshot().single { it.id == newId }
        val expectedDueAt = target.atTime(9, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedDueAt, inserted.dueAt)
        assertTrue(inserted.dueAtHasTime)
    }

    @Test
    fun `materializeProjectedOccurrence resolves root when called with child id`() = runTest {
        // Calendar UI may render projection off the latest generated child. Caller passes its
        // id; the helper must still anchor to the chain root.
        val baseDate = LocalDate.of(2026, 5, 4)
        repository.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = baseDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
            ),
            TestTaskFactory.createTask(
                id = 2L,
                title = "Standup",
                dueAt = baseDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
                parentRecurringTaskId = 1L,
            ),
        )
        val target = LocalDate.of(2026, 5, 6)

        val newId = taskManager.materializeProjectedOccurrence(parentId = 2L, date = target, zone = zone)

        val inserted = repository.getAllTasksSnapshot().single { it.id == newId }
        assertEquals(1L, inserted.parentRecurringTaskId) // root, not the child we passed in
    }

    @Test
    fun `materializeProjectedOccurrence returns null for non-recurring parent`() = runTest {
        repository.seed(
            TestTaskFactory.createTask(id = 1L, title = "One-off", recurrenceType = RecurrenceType.NONE.value),
        )

        val result = taskManager.materializeProjectedOccurrence(
            parentId = 1L,
            date = LocalDate.of(2026, 5, 10),
            zone = zone,
        )

        assertNull(result)
        assertEquals(1, repository.getAllTasksSnapshot().size) // no row inserted
    }

    @Test
    fun `materializeProjectedOccurrence returns null for archived parent`() = runTest {
        val baseDate = LocalDate.of(2026, 5, 4)
        repository.seed(
            TestTaskFactory.createTask(
                id = 1L,
                title = "Standup",
                dueAt = baseDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                recurrenceType = RecurrenceType.DAILY.value,
                isArchived = true,
            ),
        )

        val result = taskManager.materializeProjectedOccurrence(
            parentId = 1L,
            date = LocalDate.of(2026, 5, 10),
            zone = zone,
        )

        assertNull(result)
    }

    @Test
    fun `materializeProjectedOccurrence returns null for unknown parent id`() = runTest {
        val result = taskManager.materializeProjectedOccurrence(
            parentId = 999L,
            date = LocalDate.of(2026, 5, 10),
            zone = zone,
        )

        assertNull(result)
    }
}
