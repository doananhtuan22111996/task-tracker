package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonBackupSerializerTest {

    private lateinit var serializer: JsonBackupSerializer

    @Before
    fun setup() {
        serializer = JsonBackupSerializer()
    }

    @Test
    fun `round trip serialize and deserialize preserves all fields`() {
        val tasks = listOf(
            TaskBackupDto(
                id = 1L,
                title = "Buy groceries",
                description = "Milk, eggs, bread",
                isCompleted = false,
                createdAt = 1700000000000L,
                completedAt = null,
                dueAt = 1700100000000L,
                reminderOffsetMinutes = 60,
                tag = "shopping",
                isPinned = true,
                priority = 2,
                isArchived = false,
                archivedAt = null,
            ),
            TaskBackupDto(
                id = 2L,
                title = "Read book",
                description = "",
                isCompleted = true,
                createdAt = 1700000001000L,
                completedAt = 1700050000000L,
                dueAt = null,
                reminderOffsetMinutes = null,
                tag = null,
                isPinned = false,
                priority = 0,
                isArchived = true,
                archivedAt = 1700060000000L,
            ),
        )

        val json = serializer.serialize(
            tasks = tasks,
            schemaVersion = 1,
            exportedAt = 1700000000000L,
            appVersion = "1.0.0",
        )

        val deserialized = serializer.deserialize(json)

        assertEquals(2, deserialized.size)
        assertEquals(tasks[0], deserialized[0])
        assertEquals(tasks[1], deserialized[1])
    }

    @Test
    fun `deserialize handles missing optional fields with defaults`() {
        val json = """
        {
            "schemaVersion": 1,
            "exportedAt": 1700000000000,
            "appVersion": "1.0.0",
            "taskCount": 1,
            "tasks": [
                {
                    "id": 1,
                    "title": "Minimal task"
                }
            ]
        }
        """.trimIndent()

        val result = serializer.deserialize(json)

        assertEquals(1, result.size)
        val task = result[0]
        assertEquals(1L, task.id)
        assertEquals("Minimal task", task.title)
        assertEquals("", task.description)
        assertEquals(false, task.isCompleted)
        assertEquals(0L, task.createdAt)
        assertNull(task.completedAt)
        assertNull(task.dueAt)
        assertNull(task.reminderOffsetMinutes)
        assertNull(task.tag)
        assertEquals(false, task.isPinned)
        assertEquals(1, task.priority)
        assertEquals(false, task.isArchived)
        assertNull(task.archivedAt)
    }

    @Test
    fun `deserialize ignores unknown keys`() {
        val json = """
        {
            "schemaVersion": 1,
            "exportedAt": 1700000000000,
            "appVersion": "1.0.0",
            "taskCount": 1,
            "unknownField": "should be ignored",
            "tasks": [
                {
                    "id": 1,
                    "title": "Test task",
                    "futureField": 42
                }
            ]
        }
        """.trimIndent()

        val result = serializer.deserialize(json)

        assertEquals(1, result.size)
        assertEquals("Test task", result[0].title)
    }

    @Test(expected = BackupParseException::class)
    fun `deserialize throws BackupParseException for malformed JSON`() {
        serializer.deserialize("{ not valid json }")
    }

    @Test(expected = BackupParseException::class)
    fun `deserialize throws BackupParseException for empty string`() {
        serializer.deserialize("")
    }

    @Test
    fun `serialize produces valid JSON with metadata`() {
        val tasks = listOf(
            TaskBackupDto(id = 1L, title = "Test"),
        )

        val json = serializer.serialize(
            tasks = tasks,
            schemaVersion = 1,
            exportedAt = 1700000000000L,
            appVersion = "2.0.0",
        )

        assertTrue(json.contains("\"schemaVersion\""))
        assertTrue(json.contains("1700000000000"))
        assertTrue(json.contains("\"appVersion\""))
        assertTrue(json.contains("2.0.0"))
        assertTrue(json.contains("\"taskCount\""))
        assertTrue(json.contains("\"title\""))
        assertTrue(json.contains("Test"))

        // Verify round-trip to confirm structure is valid
        val deserialized = serializer.deserialize(json)
        assertEquals(1, deserialized.size)
        assertEquals("Test", deserialized[0].title)
    }

    @Test
    fun `round trip with empty task list`() {
        val json = serializer.serialize(
            tasks = emptyList(),
            schemaVersion = 1,
            exportedAt = 1700000000000L,
            appVersion = "1.0.0",
        )

        val result = serializer.deserialize(json)
        assertTrue(result.isEmpty())
    }

    @Test(expected = BackupParseException::class)
    fun `deserialize rejects schema version greater than current`() {
        val json = """
        {
            "schemaVersion": 99,
            "exportedAt": 1700000000000,
            "appVersion": "9.0.0",
            "taskCount": 1,
            "tasks": [
                {
                    "id": 1,
                    "title": "Future task"
                }
            ]
        }
        """.trimIndent()

        serializer.deserialize(json)
    }

    @Test
    fun `deserialize accepts current schema version`() {
        val json = """
        {
            "schemaVersion": 1,
            "exportedAt": 1700000000000,
            "appVersion": "1.0.0",
            "taskCount": 1,
            "tasks": [
                {
                    "id": 1,
                    "title": "Current schema task"
                }
            ]
        }
        """.trimIndent()

        val result = serializer.deserialize(json)
        assertEquals(1, result.size)
        assertEquals("Current schema task", result[0].title)
    }

    @Test
    fun `serialize encodes default values`() {
        val task = TaskBackupDto(id = 5L, title = "Defaults test")
        val json = serializer.serialize(
            tasks = listOf(task),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        // encodeDefaults = true, so all fields should be present
        assertTrue(json.contains("\"description\""))
        assertTrue(json.contains("\"isCompleted\""))
        assertTrue(json.contains("\"isPinned\""))
        assertTrue(json.contains("\"priority\""))

        // Verify values through round-trip
        val deserialized = serializer.deserialize(json)
        assertEquals(1, deserialized.size)
        assertEquals("", deserialized[0].description)
        assertEquals(false, deserialized[0].isCompleted)
        assertEquals(false, deserialized[0].isPinned)
        assertEquals(1, deserialized[0].priority)
    }
}
