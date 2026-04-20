package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round-trip tests that verify tag values are normalized to canonical UPPERCASE
 * when tasks are imported, regardless of the source format (JSON or CSV v1/v2/current).
 */
class TagNormalizationBackupTest {

    private val csvSerializer = CsvBackupSerializer()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // === JSON import normalizes tag ===

    @Test
    fun `json import uppercases lowercase tag on toTask`() {
        val dto = TaskBackupDto(id = 1L, title = "A", tag = "work", tagColor = "blue")
        val jsonStr = json.encodeToString(dto)
        val parsed = json.decodeFromString<TaskBackupDto>(jsonStr)

        val task = parsed.toTask()
        assertEquals("WORK", task.tag)
        assertEquals("blue", task.tagColor)
    }

    @Test
    fun `json import trims and uppercases padded mixed case tag`() {
        val dto = TaskBackupDto(id = 1L, title = "A", tag = "  Work  ")
        val task = json.decodeFromString<TaskBackupDto>(json.encodeToString(dto)).toTask()
        assertEquals("WORK", task.tag)
    }

    @Test
    fun `json import converts blank tag to null and clears color`() {
        val dto = TaskBackupDto(id = 1L, title = "A", tag = "   ", tagColor = "red")
        val task = json.decodeFromString<TaskBackupDto>(json.encodeToString(dto)).toTask()
        assertNull(task.tag)
        assertNull(task.tagColor)
    }

    @Test
    fun `json import keeps null tag as null`() {
        val dto = TaskBackupDto(id = 1L, title = "A", tag = null, tagColor = null)
        val task = json.decodeFromString<TaskBackupDto>(json.encodeToString(dto)).toTask()
        assertNull(task.tag)
        assertNull(task.tagColor)
    }

    // === CSV (current 20-col) import normalizes tag ===

    @Test
    fun `csv current format import uppercases tag`() {
        val dto = TaskBackupDto(id = 1L, title = "A", tag = "work", tagColor = "blue")
        val csv = csvSerializer.serialize(listOf(dto), 2, 0L, "1.9.0")
        val task = csvSerializer.deserialize(csv)[0].toTask()

        assertEquals("WORK", task.tag)
        assertEquals("blue", task.tagColor)
    }

    @Test
    fun `csv current format import trims and uppercases padded tag`() {
        // Tag contains leading/trailing whitespace — wrap in quotes per RFC 4180 to preserve
        val csv = """
            id,title,description,isCompleted,createdAt,completedAt,dueAt,dueAtHasTime,reminderOffsetMinutes,tag,tagColor,isPinned,priority,isArchived,archivedAt,recurrenceType,recurrenceInterval,recurrenceDaysOfWeek,recurrenceEndDate,parentRecurringTaskId
            1,Task,,false,1700000000000,,,false,,"  Work  ",,false,1,false,,0,1,0,,
        """.trimIndent()

        val task = csvSerializer.deserialize(csv)[0].toTask()
        assertEquals("WORK", task.tag)
    }

    // === CSV legacy format (v2 19-col, pre-color) import normalizes tag ===

    @Test
    fun `csv v2 19-column legacy import uppercases tag`() {
        val legacyCsv = """
            id,title,description,isCompleted,createdAt,completedAt,dueAt,dueAtHasTime,reminderOffsetMinutes,tag,isPinned,priority,isArchived,archivedAt,recurrenceType,recurrenceInterval,recurrenceDaysOfWeek,recurrenceEndDate,parentRecurringTaskId
            1,Task,,false,1700000000000,,,false,,work,false,1,false,,0,1,0,,
        """.trimIndent()

        val task = csvSerializer.deserialize(legacyCsv)[0].toTask()
        assertEquals("WORK", task.tag)
        assertNull(task.tagColor)
    }

    // === CSV v1 14-column legacy import normalizes tag ===

    @Test
    fun `csv v1 14-column legacy import uppercases tag`() {
        val v1Csv = """
            id,title,description,isCompleted,createdAt,completedAt,dueAt,dueAtHasTime,reminderOffsetMinutes,tag,isPinned,priority,isArchived,archivedAt
            1,Task,,false,1700000000000,,,false,,Personal,false,1,false,
        """.trimIndent()

        val task = csvSerializer.deserialize(v1Csv)[0].toTask()
        assertEquals("PERSONAL", task.tag)
    }

    // === fromTask pass-through (export does not re-normalize) ===

    @Test
    fun `fromTask passes tag through unchanged on export`() {
        // Storage is already canonical, so export should not re-uppercase.
        val task = dev.tuandoan.tasktracker.data.database.Task(
            id = 1L,
            title = "A",
            tag = "WORK",
            tagColor = "blue",
        )
        val dto = TaskBackupDto.fromTask(task)
        assertEquals("WORK", dto.tag)
        assertEquals("blue", dto.tagColor)
    }
}
