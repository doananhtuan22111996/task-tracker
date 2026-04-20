package dev.tuandoan.tasktracker.data.backup.dto

import dev.tuandoan.tasktracker.data.database.Task
import dev.tuandoan.tasktracker.testutil.TestTaskFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskBackupDtoTest {

    // === fromTask ===

    @Test
    fun `fromTask maps all fields correctly`() {
        val task = Task(
            id = 42L,
            title = "Test Title",
            description = "Test Desc",
            isCompleted = true,
            createdAt = 1700000000000L,
            completedAt = 1700050000000L,
            dueAt = 1700100000000L,
            reminderOffsetMinutes = 60,
            tag = "work",
            isPinned = true,
            priority = 2,
            isArchived = true,
            archivedAt = 1700200000000L,
        )

        val dto = TaskBackupDto.fromTask(task)

        assertEquals(42L, dto.id)
        assertEquals("Test Title", dto.title)
        assertEquals("Test Desc", dto.description)
        assertTrue(dto.isCompleted)
        assertEquals(1700000000000L, dto.createdAt)
        assertEquals(1700050000000L, dto.completedAt)
        assertEquals(1700100000000L, dto.dueAt)
        assertEquals(60, dto.reminderOffsetMinutes)
        assertEquals("work", dto.tag)
        assertTrue(dto.isPinned)
        assertEquals(2, dto.priority)
        assertTrue(dto.isArchived)
        assertEquals(1700200000000L, dto.archivedAt)
    }

    @Test
    fun `fromTask maps null fields correctly`() {
        val task = TestTaskFactory.createTask(
            id = 1,
            completedAt = null,
            dueAt = null,
            reminderOffsetMinutes = null,
            tag = null,
            archivedAt = null,
        )

        val dto = TaskBackupDto.fromTask(task)

        assertNull(dto.completedAt)
        assertNull(dto.dueAt)
        assertNull(dto.reminderOffsetMinutes)
        assertNull(dto.tag)
        assertNull(dto.archivedAt)
    }

    // === toTask ===

    @Test
    fun `toTask maps all fields correctly`() {
        val dto = TaskBackupDto(
            id = 42L,
            title = "Dto Title",
            description = "Dto Desc",
            isCompleted = true,
            createdAt = 1700000000000L,
            completedAt = 1700050000000L,
            dueAt = 1700100000000L,
            reminderOffsetMinutes = 5,
            tag = "home",
            isPinned = false,
            priority = 0,
            isArchived = true,
            archivedAt = 1700200000000L,
        )

        val task = dto.toTask()

        assertEquals(42L, task.id)
        assertEquals("Dto Title", task.title)
        assertEquals("Dto Desc", task.description)
        assertTrue(task.isCompleted)
        assertEquals(1700000000000L, task.createdAt)
        assertEquals(1700050000000L, task.completedAt)
        assertEquals(1700100000000L, task.dueAt)
        assertEquals(5, task.reminderOffsetMinutes)
        assertEquals("HOME", task.tag)
        assertFalse(task.isPinned)
        assertEquals(0, task.priority)
        assertTrue(task.isArchived)
        assertEquals(1700200000000L, task.archivedAt)
    }

    @Test
    fun `toTask maps null fields correctly`() {
        val dto = TaskBackupDto(
            id = 1L,
            title = "Minimal",
        )

        val task = dto.toTask()

        assertEquals("", task.description)
        assertFalse(task.isCompleted)
        assertEquals(0L, task.createdAt)
        assertNull(task.completedAt)
        assertNull(task.dueAt)
        assertNull(task.reminderOffsetMinutes)
        assertNull(task.tag)
        assertFalse(task.isPinned)
        assertEquals(1, task.priority)
        assertFalse(task.isArchived)
        assertNull(task.archivedAt)
    }

    // === Round trip ===

    @Test
    fun `fromTask then toTask preserves all data`() {
        val original = Task(
            id = 99L,
            title = "Round Trip",
            description = "Desc",
            isCompleted = true,
            createdAt = 1700000000000L,
            completedAt = 1700050000000L,
            dueAt = 1700100000000L,
            reminderOffsetMinutes = 1440,
            tag = "IMPORTANT",
            isPinned = true,
            priority = 2,
            isArchived = true,
            archivedAt = 1700200000000L,
        )

        val roundTripped = TaskBackupDto.fromTask(original).toTask()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.title, roundTripped.title)
        assertEquals(original.description, roundTripped.description)
        assertEquals(original.isCompleted, roundTripped.isCompleted)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.completedAt, roundTripped.completedAt)
        assertEquals(original.dueAt, roundTripped.dueAt)
        assertEquals(original.reminderOffsetMinutes, roundTripped.reminderOffsetMinutes)
        assertEquals(original.tag, roundTripped.tag)
        assertEquals(original.isPinned, roundTripped.isPinned)
        assertEquals(original.priority, roundTripped.priority)
        assertEquals(original.isArchived, roundTripped.isArchived)
        assertEquals(original.archivedAt, roundTripped.archivedAt)
    }

    // === DTO defaults ===

    @Test
    fun `DTO default values are correct`() {
        val dto = TaskBackupDto(id = 1L, title = "Test")

        assertEquals("", dto.description)
        assertFalse(dto.isCompleted)
        assertEquals(0L, dto.createdAt)
        assertNull(dto.completedAt)
        assertNull(dto.dueAt)
        assertNull(dto.reminderOffsetMinutes)
        assertNull(dto.tag)
        assertFalse(dto.isPinned)
        assertEquals(1, dto.priority)
        assertFalse(dto.isArchived)
        assertNull(dto.archivedAt)
    }
}
