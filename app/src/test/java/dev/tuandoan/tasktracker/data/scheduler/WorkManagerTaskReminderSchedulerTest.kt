package dev.tuandoan.tasktracker.data.scheduler

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkManagerTaskReminderSchedulerTest {

    @Test
    fun `unique work name uses stable task_reminder_id format`() {
        assertEquals("task_reminder_1", WorkManagerTaskReminderScheduler.uniqueWorkName(1L))
        assertEquals("task_reminder_42", WorkManagerTaskReminderScheduler.uniqueWorkName(42L))
        assertEquals(
            "task_reminder_9999999999",
            WorkManagerTaskReminderScheduler.uniqueWorkName(9_999_999_999L),
        )
    }
}
