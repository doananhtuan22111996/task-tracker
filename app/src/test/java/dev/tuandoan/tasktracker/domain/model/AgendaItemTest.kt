package dev.tuandoan.tasktracker.domain.model

import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class AgendaItemTest {

    private val day = LocalDate.of(2026, 5, 10)

    @Test
    fun `Concrete exposes its date via the common property`() {
        val task = TestTaskFactory.createTask(id = 1L, title = "Standup")
        val item: AgendaItem = AgendaItem.Concrete(task = task, date = day)

        assertEquals(day, item.date)
        assertEquals(task, (item as AgendaItem.Concrete).task)
    }

    @Test
    fun `Projected exposes its date via the common property`() {
        val item: AgendaItem = AgendaItem.Projected(
            parentTaskId = 7L,
            date = day,
            title = "Standup",
            description = "",
            priority = 1,
            tag = null,
            tagColor = null,
            dueAtHasTime = false,
            reminderOffsetMinutes = null,
            recurrenceType = 1,
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = 0,
            recurrenceEndDate = null,
        )

        assertEquals(day, item.date)
        assertEquals(7L, (item as AgendaItem.Projected).parentTaskId)
    }

    @Test
    fun `Concrete and Projected on the same day are not equal`() {
        val concrete: AgendaItem = AgendaItem.Concrete(
            task = TestTaskFactory.createTask(id = 1L, title = "Standup"),
            date = day,
        )
        val projected: AgendaItem = AgendaItem.Projected(
            parentTaskId = 1L,
            date = day,
            title = "Standup",
            description = "",
            priority = 1,
            tag = null,
            tagColor = null,
            dueAtHasTime = false,
            reminderOffsetMinutes = null,
            recurrenceType = 1,
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = 0,
            recurrenceEndDate = null,
        )

        assertNotEquals(concrete, projected)
    }
}
