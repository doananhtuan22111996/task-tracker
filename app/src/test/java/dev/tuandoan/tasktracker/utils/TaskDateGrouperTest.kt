package dev.tuandoan.tasktracker.utils

import android.content.Context
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class TaskDateGrouperTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getString(R.string.date_today) } returns "Today"
        every { context.getString(R.string.date_yesterday) } returns "Yesterday"
        every { context.getString(R.string.date_tomorrow) } returns "Tomorrow"
    }

    // === empty list ===

    @Test
    fun `groupTasksByDay with empty list returns empty`() {
        val result = TaskDateGrouper.groupTasksByDay(emptyList(), context)
        assertTrue(result.isEmpty())
    }

    // === grouping by date key ===

    @Test
    fun `tasks on same day are grouped together`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 15, 10, 0, 0)
        val time1 = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 15, 14, 30, 0)
        val time2 = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Morning", createdAt = time1),
            TestTaskFactory.createTask(id = 2, title = "Afternoon", createdAt = time2),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals(2, sections[0].tasks.size)
        assertEquals("2024-03-15", sections[0].dateKey)
    }

    @Test
    fun `tasks on different days create separate sections`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 15, 10, 0, 0)
        val day1 = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 16, 10, 0, 0)
        val day2 = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Day 1", createdAt = day1),
            TestTaskFactory.createTask(id = 2, title = "Day 2", createdAt = day2),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(2, sections.size)
    }

    // === sections sorted most recent first ===

    @Test
    fun `sections are sorted by date descending`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.JANUARY, 10, 10, 0, 0)
        val oldDay = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 20, 10, 0, 0)
        val newDay = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Old", createdAt = oldDay),
            TestTaskFactory.createTask(id = 2, title = "New", createdAt = newDay),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(2, sections.size)
        assertEquals("2024-03-20", sections[0].dateKey)
        assertEquals("2024-01-10", sections[1].dateKey)
    }

    // === dueAt takes priority over createdAt for grouping ===

    @Test
    fun `task with dueAt is grouped by due date not created date`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 10, 10, 0, 0)
        val createdAt = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 20, 10, 0, 0)
        val dueAt = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Due later", createdAt = createdAt, dueAt = dueAt),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals("2024-03-20", sections[0].dateKey)
    }

    @Test
    fun `task without dueAt uses createdAt for grouping`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 10, 10, 0, 0)
        val createdAt = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "No due", createdAt = createdAt, dueAt = null),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals("2024-03-10", sections[0].dateKey)
    }

    // === Pinned-first ordering within sections ===

    @Test
    fun `pinned tasks appear before unpinned within same section`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 15, 10, 0, 0)
        val timestamp = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Unpinned", createdAt = timestamp, isPinned = false),
            TestTaskFactory.createTask(id = 2, title = "Pinned", createdAt = timestamp + 1000, isPinned = true),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals("Pinned", sections[0].tasks[0].title)
        assertEquals("Unpinned", sections[0].tasks[1].title)
    }

    // === Header labels ===

    @Test
    fun `today header label is Today`() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Today task", createdAt = now),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals("Today", sections[0].header)
    }

    @Test
    fun `yesterday header label is Yesterday`() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Yesterday task", createdAt = yesterday),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals("Yesterday", sections[0].header)
    }

    @Test
    fun `tomorrow header label is Tomorrow`() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Tomorrow task", dueAt = tomorrow, createdAt = 1000L),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        assertEquals("Tomorrow", sections[0].header)
    }

    @Test
    fun `older date uses formatted date string`() {
        val cal = Calendar.getInstance()
        cal.set(2023, Calendar.JUNE, 5, 10, 0, 0)
        val oldDate = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Old task", createdAt = oldDate),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals(1, sections.size)
        // The header should be a formatted date like "Jun 5, 2023"
        assertTrue(
            "Header should contain year 2023, got: ${sections[0].header}",
            sections[0].header.contains("2023"),
        )
    }

    // === dateKey format ===

    // === Priority grouping ===

    @Test
    fun `groupByPriority returns High Medium Low order`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Low", priority = 0),
            TestTaskFactory.createTask(id = 2, title = "High", priority = 2),
            TestTaskFactory.createTask(id = 3, title = "Medium", priority = 1),
        )

        val sections = TaskDateGrouper.groupByPriority(tasks)

        assertEquals(3, sections.size)
        assertEquals("\uD83D\uDD34 High", sections[0].header)
        assertEquals("\uD83D\uDFE1 Medium", sections[1].header)
        assertEquals("\uD83D\uDFE2 Low", sections[2].header)
    }

    @Test
    fun `groupByPriority omits empty priority sections`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "High only", priority = 2),
        )

        val sections = TaskDateGrouper.groupByPriority(tasks)

        assertEquals(1, sections.size)
        assertEquals("\uD83D\uDD34 High", sections[0].header)
    }

    @Test
    fun `groupByPriority pinned tasks appear first within each section`() {
        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, title = "Unpinned High", priority = 2, isPinned = false),
            TestTaskFactory.createTask(id = 2, title = "Pinned High", priority = 2, isPinned = true),
        )

        val sections = TaskDateGrouper.groupByPriority(tasks)

        assertEquals(1, sections.size)
        assertEquals("Pinned High", sections[0].tasks[0].title)
        assertEquals("Unpinned High", sections[0].tasks[1].title)
    }

    @Test
    fun `groupByPriority with empty list returns empty`() {
        val result = TaskDateGrouper.groupByPriority(emptyList())
        assertTrue(result.isEmpty())
    }

    // === dateKey format ===

    @Test
    fun `dateKey format is YYYY-MM-DD with zero padding`() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.JANUARY, 5, 10, 0, 0) // Jan 5
        val timestamp = cal.timeInMillis

        val tasks = listOf(
            TestTaskFactory.createTask(id = 1, createdAt = timestamp),
        )

        val sections = TaskDateGrouper.groupTasksByDay(tasks, context)

        assertEquals("2024-01-05", sections[0].dateKey)
    }
}
