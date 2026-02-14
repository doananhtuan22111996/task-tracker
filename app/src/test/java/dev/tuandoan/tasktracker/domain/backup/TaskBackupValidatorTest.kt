package dev.tuandoan.tasktracker.domain.backup

import dev.tuandoan.tasktracker.data.database.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskBackupValidatorTest {

    private lateinit var validator: TaskBackupValidator

    @Before
    fun setup() {
        validator = TaskBackupValidator()
    }

    @Test
    fun `valid tasks pass through unchanged`() {
        val tasks = listOf(
            Task(
                id = 1L,
                title = "Valid task",
                description = "desc",
                isCompleted = false,
                createdAt = 1700000000000L,
                priority = 1,
            ),
        )

        val result = validator.validate(tasks)

        assertEquals(1, result.validTasks.size)
        assertEquals(0, result.skippedCount)
        assertEquals("Valid task", result.validTasks[0].title)
    }

    @Test
    fun `tasks with blank titles are skipped`() {
        val tasks = listOf(
            Task(id = 1L, title = ""),
            Task(id = 2L, title = "   "),
            Task(id = 3L, title = "Good task"),
        )

        val result = validator.validate(tasks)

        assertEquals(1, result.validTasks.size)
        assertEquals(2, result.skippedCount)
        assertEquals("Good task", result.validTasks[0].title)
    }

    @Test
    fun `priority is clamped to valid range 0-2`() {
        val tasks = listOf(
            Task(id = 1L, title = "Low priority", priority = -5),
            Task(id = 2L, title = "High priority", priority = 10),
            Task(id = 3L, title = "Normal priority", priority = 1),
        )

        val result = validator.validate(tasks)

        assertEquals(3, result.validTasks.size)
        assertEquals(0, result.validTasks[0].priority)
        assertEquals(2, result.validTasks[1].priority)
        assertEquals(1, result.validTasks[2].priority)
    }

    @Test
    fun `createdAt defaults to current time when zero or negative`() {
        val tasks = listOf(
            Task(id = 1L, title = "Zero created", createdAt = 0L),
            Task(id = 2L, title = "Negative created", createdAt = -100L),
        )

        val beforeValidation = System.currentTimeMillis()
        val result = validator.validate(tasks)
        val afterValidation = System.currentTimeMillis()

        assertEquals(2, result.validTasks.size)
        for (task in result.validTasks) {
            assertTrue(
                "createdAt should be set to current time",
                task.createdAt in beforeValidation..afterValidation,
            )
        }
    }

    @Test
    fun `positive createdAt is preserved`() {
        val tasks = listOf(
            Task(id = 1L, title = "Has time", createdAt = 1700000000000L),
        )

        val result = validator.validate(tasks)

        assertEquals(1700000000000L, result.validTasks[0].createdAt)
    }

    @Test
    fun `completedAt is nulled when isCompleted is false`() {
        val tasks = listOf(
            Task(
                id = 1L,
                title = "Not completed but has timestamp",
                isCompleted = false,
                completedAt = 1700000000000L,
            ),
        )

        val result = validator.validate(tasks)

        assertNull(result.validTasks[0].completedAt)
    }

    @Test
    fun `completedAt is preserved when isCompleted is true`() {
        val tasks = listOf(
            Task(
                id = 1L,
                title = "Completed",
                isCompleted = true,
                completedAt = 1700000000000L,
            ),
        )

        val result = validator.validate(tasks)

        assertEquals(1700000000000L, result.validTasks[0].completedAt)
    }

    @Test
    fun `archivedAt is nulled when isArchived is false`() {
        val tasks = listOf(
            Task(
                id = 1L,
                title = "Not archived but has timestamp",
                isArchived = false,
                archivedAt = 1700000000000L,
            ),
        )

        val result = validator.validate(tasks)

        assertNull(result.validTasks[0].archivedAt)
    }

    @Test
    fun `archivedAt is preserved when isArchived is true`() {
        val tasks = listOf(
            Task(
                id = 1L,
                title = "Archived",
                isArchived = true,
                archivedAt = 1700000000000L,
            ),
        )

        val result = validator.validate(tasks)

        assertEquals(1700000000000L, result.validTasks[0].archivedAt)
    }

    @Test
    fun `empty list returns empty result`() {
        val result = validator.validate(emptyList())

        assertTrue(result.validTasks.isEmpty())
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `multiple validation rules applied together`() {
        val tasks = listOf(
            Task(
                id = 1L,
                title = "Fix all",
                isCompleted = false,
                completedAt = 999L,
                isArchived = false,
                archivedAt = 888L,
                priority = 99,
                createdAt = -1L,
            ),
        )

        val result = validator.validate(tasks)

        assertEquals(1, result.validTasks.size)
        val task = result.validTasks[0]
        assertEquals(2, task.priority) // Clamped to max
        assertNull(task.completedAt) // Nulled because not completed
        assertNull(task.archivedAt) // Nulled because not archived
        assertTrue(task.createdAt > 0L) // Set to current time
    }

    @Test
    fun `title is truncated to 100 characters`() {
        val longTitle = "A".repeat(200)
        val tasks = listOf(
            Task(id = 1L, title = longTitle, createdAt = 1700000000000L),
        )

        val result = validator.validate(tasks)

        assertEquals(1, result.validTasks.size)
        assertEquals(100, result.validTasks[0].title.length)
        assertEquals("A".repeat(100), result.validTasks[0].title)
    }

    @Test
    fun `description is truncated to 500 characters`() {
        val longDescription = "B".repeat(1000)
        val tasks = listOf(
            Task(id = 1L, title = "Valid", description = longDescription, createdAt = 1700000000000L),
        )

        val result = validator.validate(tasks)

        assertEquals(1, result.validTasks.size)
        assertEquals(500, result.validTasks[0].description.length)
        assertEquals("B".repeat(500), result.validTasks[0].description)
    }

    @Test
    fun `title and description within limits are preserved`() {
        val title = "A".repeat(100)
        val description = "B".repeat(500)
        val tasks = listOf(
            Task(id = 1L, title = title, description = description, createdAt = 1700000000000L),
        )

        val result = validator.validate(tasks)

        assertEquals(title, result.validTasks[0].title)
        assertEquals(description, result.validTasks[0].description)
    }
}
