package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DueAtHasTimeBackupTest {

    private val csvSerializer = CsvBackupSerializer()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // === CSV Tests ===

    @Test
    fun `csv round trip preserves dueAtHasTime true`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Meeting",
            dueAt = 1700000000000L,
            dueAtHasTime = true,
            reminderOffsetMinutes = 60,
        )

        val csv = csvSerializer.serialize(listOf(task), 1, 0L, "1.0.0")
        val deserialized = csvSerializer.deserialize(csv)

        assertEquals(1, deserialized.size)
        assertTrue(deserialized[0].dueAtHasTime)
        assertEquals(1700000000000L, deserialized[0].dueAt)
    }

    @Test
    fun `csv round trip preserves dueAtHasTime false`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Task",
            dueAt = 1700000000000L,
            dueAtHasTime = false,
        )

        val csv = csvSerializer.serialize(listOf(task), 1, 0L, "1.0.0")
        val deserialized = csvSerializer.deserialize(csv)

        assertFalse(deserialized[0].dueAtHasTime)
    }

    @Test
    fun `csv import of legacy 13-column format defaults dueAtHasTime to false`() {
        val legacyCsv = """
            id,title,description,isCompleted,createdAt,completedAt,dueAt,reminderOffsetMinutes,tag,isPinned,priority,isArchived,archivedAt
            1,Task,,false,1700000000000,,1700100000000,60,,false,1,false,
        """.trimIndent()

        val deserialized = csvSerializer.deserialize(legacyCsv)

        assertEquals(1, deserialized.size)
        assertFalse(deserialized[0].dueAtHasTime)
        assertEquals(1700100000000L, deserialized[0].dueAt)
        assertEquals(60, deserialized[0].reminderOffsetMinutes)
    }

    @Test
    fun `csv header includes dueAtHasTime column`() {
        val csv = csvSerializer.serialize(
            listOf(TaskBackupDto(id = 1L, title = "Test")),
            1,
            0L,
            "1.0.0",
        )

        val header = csv.lines().first()
        assertTrue(header.contains("dueAtHasTime"))
    }

    // === JSON Tests ===

    @Test
    fun `json round trip preserves dueAtHasTime`() {
        val dto = TaskBackupDto(
            id = 1L,
            title = "Meeting",
            dueAt = 1700000000000L,
            dueAtHasTime = true,
            reminderOffsetMinutes = 60,
        )

        val jsonStr = json.encodeToString(dto)
        val deserialized = json.decodeFromString<TaskBackupDto>(jsonStr)

        assertTrue(deserialized.dueAtHasTime)
    }

    @Test
    fun `json import without dueAtHasTime field defaults to false`() {
        val legacyJson = """
            {"id":1,"title":"Task","description":"","isCompleted":false,"createdAt":0,"completedAt":null,"dueAt":1700000000000,"reminderOffsetMinutes":60,"tag":null,"isPinned":false,"priority":1,"isArchived":false,"archivedAt":null}
        """.trimIndent()

        val deserialized = json.decodeFromString<TaskBackupDto>(legacyJson)

        assertFalse(deserialized.dueAtHasTime)
        assertEquals(1700000000000L, deserialized.dueAt)
    }

    // === DTO Mapping Tests ===

    @Test
    fun `toTask maps dueAtHasTime correctly`() {
        val dto = TaskBackupDto(
            id = 1L,
            title = "Task",
            dueAt = 1700000000000L,
            dueAtHasTime = true,
        )

        val task = dto.toTask()
        assertTrue(task.dueAtHasTime)
    }

    @Test
    fun `fromTask maps dueAtHasTime correctly`() {
        val task = dev.tuandoan.tasktracker.data.database.Task(
            id = 1L,
            title = "Task",
            dueAt = 1700000000000L,
            dueAtHasTime = true,
        )

        val dto = TaskBackupDto.fromTask(task)
        assertTrue(dto.dueAtHasTime)
    }
}
