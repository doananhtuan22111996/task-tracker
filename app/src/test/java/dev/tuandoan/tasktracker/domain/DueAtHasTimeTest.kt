package dev.tuandoan.tasktracker.domain

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.domain.model.DueDatePreset
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DueAtHasTimeTest {

    // === Task Entity Tests ===

    @Test
    fun `task dueAtHasTime defaults to false`() {
        val task = Task(title = "Test")
        assertFalse(task.dueAtHasTime)
    }

    @Test
    fun `task copy preserves dueAtHasTime`() {
        val task = TestTaskFactory.createTask(dueAt = 1700000000000L, dueAtHasTime = true)
        val copied = task.copy(title = "New Title")
        assertTrue(copied.dueAtHasTime)
    }

    @Test
    fun `task copy can change dueAtHasTime`() {
        val task = TestTaskFactory.createTask(dueAt = 1700000000000L, dueAtHasTime = true)
        val cleared = task.copy(dueAtHasTime = false)
        assertFalse(cleared.dueAtHasTime)
    }

    @Test
    fun `TestTaskFactory creates task with dueAtHasTime`() {
        val task = TestTaskFactory.createTask(dueAtHasTime = true, dueAt = 1700000000000L)
        assertTrue(task.dueAtHasTime)
    }

    @Test
    fun `TestTaskFactory taskWithDueDate defaults dueAtHasTime to false`() {
        val task = TestTaskFactory.taskWithDueDate()
        assertFalse(task.dueAtHasTime)
    }

    // === DueDatePreset Tests ===

    @Test
    fun `DueDatePreset TODAY sets time to 23 59`() {
        val millis = DueDatePreset.TODAY.toEpochMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `DueDatePreset TOMORROW sets time to 23 59`() {
        val millis = DueDatePreset.TOMORROW.toEpochMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `DueDatePreset NEXT_WEEK sets time to 23 59`() {
        val millis = DueDatePreset.NEXT_WEEK.toEpochMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }
}
