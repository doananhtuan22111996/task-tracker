package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.SubtaskBackupDto
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Round-trip tests covering subtask serialization across JSON and CSV, plus backward
 * compatibility with v1/v2 backups that pre-date the subtasks field.
 */
class SubtaskBackupTest {

    private lateinit var jsonSerializer: JsonBackupSerializer
    private lateinit var csvSerializer: CsvBackupSerializer

    @Before
    fun setup() {
        jsonSerializer = JsonBackupSerializer()
        csvSerializer = CsvBackupSerializer()
    }

    // === JSON ===

    @Test
    fun `json round-trip preserves subtasks`() {
        val tasks = listOf(
            TaskBackupDto(
                id = 1L,
                title = "Plan trip",
                createdAt = 1_700_000_000_000L,
                subtasks = listOf(
                    SubtaskBackupDto(id = 10L, title = "Book flight", isCompleted = true, sortOrder = 0),
                    SubtaskBackupDto(id = 11L, title = "Reserve hotel", isCompleted = false, sortOrder = 1),
                ),
            ),
            TaskBackupDto(id = 2L, title = "No checklist", createdAt = 1_700_000_001_000L),
        )

        val json = jsonSerializer.serialize(tasks, BackupMetadata.CURRENT_SCHEMA_VERSION, 0L, "test")
        val restored = jsonSerializer.deserialize(json)

        assertEquals(2, restored.size)
        assertEquals(
            listOf("Book flight", "Reserve hotel"),
            restored[0].subtasks.map { it.title },
        )
        assertEquals(listOf(true, false), restored[0].subtasks.map { it.isCompleted })
        assertEquals(listOf(0, 1), restored[0].subtasks.map { it.sortOrder })
        assertTrue(restored[1].subtasks.isEmpty())
    }

    @Test
    fun `json v2 backup without subtasks imports with empty subtask list`() {
        // A v2 backup predating the subtasks field — must still decode.
        val v2Json = """
            {
              "schemaVersion": 2,
              "exportedAt": 1700000000000,
              "appVersion": "1.9.0",
              "taskCount": 1,
              "tasks": [
                {
                  "id": 1,
                  "title": "Legacy task",
                  "description": "",
                  "isCompleted": false,
                  "createdAt": 1700000000000
                }
              ]
            }
        """.trimIndent()

        val restored = jsonSerializer.deserialize(v2Json)

        assertEquals(1, restored.size)
        assertTrue(restored[0].subtasks.isEmpty())
    }

    @Test
    fun `json round-trip preserves subtask titles with special characters`() {
        val tricky = "Title, with \"quotes\" and\nnewline"
        val tasks = listOf(
            TaskBackupDto(
                id = 1L,
                title = "Parent",
                createdAt = 1_700_000_000_000L,
                subtasks = listOf(SubtaskBackupDto(id = 10L, title = tricky)),
            ),
        )

        val json = jsonSerializer.serialize(tasks, BackupMetadata.CURRENT_SCHEMA_VERSION, 0L, "test")
        val restored = jsonSerializer.deserialize(json)

        assertEquals(tricky, restored[0].subtasks[0].title)
    }

    // === CSV ===

    @Test
    fun `csv round-trip preserves subtasks including special characters`() {
        val tricky = "Titled, \"quoted\"\nmulti-line"
        val tasks = listOf(
            TaskBackupDto(
                id = 1L,
                title = "Parent",
                createdAt = 1_700_000_000_000L,
                subtasks = listOf(
                    SubtaskBackupDto(id = 10L, title = tricky, isCompleted = true, sortOrder = 0),
                    SubtaskBackupDto(id = 11L, title = "Plain", isCompleted = false, sortOrder = 1),
                ),
            ),
        )

        val csv = csvSerializer.serialize(tasks, BackupMetadata.CURRENT_SCHEMA_VERSION, 0L, "test")
        val restored = csvSerializer.deserialize(csv)

        assertEquals(1, restored.size)
        assertEquals(2, restored[0].subtasks.size)
        assertEquals(tricky, restored[0].subtasks[0].title)
        assertTrue(restored[0].subtasks[0].isCompleted)
        assertEquals("Plain", restored[0].subtasks[1].title)
    }

    @Test
    fun `csv v2-color row without subtasks column parses with empty list`() {
        // 20-column row from v1.9.0 (pre-subtasks).
        val header = "id,title,description,isCompleted,createdAt,completedAt,dueAt,dueAtHasTime," +
            "reminderOffsetMinutes,tag,tagColor,isPinned,priority,isArchived,archivedAt," +
            "recurrenceType,recurrenceInterval,recurrenceDaysOfWeek,recurrenceEndDate," +
            "parentRecurringTaskId"
        val row = "1,Old task,,false,1700000000000,,,false,,,,false,1,false,,0,1,0,,"
        val csv = "$header\n$row\n"

        val restored = csvSerializer.deserialize(csv)

        assertEquals(1, restored.size)
        assertEquals("Old task", restored[0].title)
        assertTrue(restored[0].subtasks.isEmpty())
    }

    @Test
    fun `csv task with empty subtasks list emits empty cell`() {
        val tasks = listOf(
            TaskBackupDto(id = 1L, title = "Bare", createdAt = 1_700_000_000_000L),
        )

        val csv = csvSerializer.serialize(tasks, BackupMetadata.CURRENT_SCHEMA_VERSION, 0L, "test")

        // Ends with comma + newline when the final cell is empty.
        assertTrue(csv.trimEnd().endsWith(","))
        val restored = csvSerializer.deserialize(csv)
        assertTrue(restored[0].subtasks.isEmpty())
    }

    // === Metadata ===

    @Test
    fun `current schema version is 3`() {
        assertEquals(3, BackupMetadata.CURRENT_SCHEMA_VERSION)
    }
}
