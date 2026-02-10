package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsvBackupSerializerTest {

    private lateinit var serializer: CsvBackupSerializer

    @Before
    fun setup() {
        serializer = CsvBackupSerializer()
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

        val csv = serializer.serialize(
            tasks = tasks,
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        val deserialized = serializer.deserialize(csv)

        assertEquals(2, deserialized.size)
        assertEquals(tasks[0], deserialized[0])
        assertEquals(tasks[1], deserialized[1])
    }

    @Test
    fun `serialize includes header row`() {
        val csv = serializer.serialize(
            tasks = listOf(TaskBackupDto(id = 1L, title = "Test")),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        val firstLine = csv.lines().first()
        assertTrue(firstLine.startsWith("id,title,description"))
        assertTrue(firstLine.contains("isArchived,archivedAt"))
    }

    @Test
    fun `serialize handles commas in fields by quoting`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Buy milk, eggs",
            description = "From store",
        )

        val csv = serializer.serialize(
            tasks = listOf(task),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        // The title should be wrapped in quotes
        assertTrue(csv.contains("\"Buy milk, eggs\""))

        // Verify round-trip works
        val deserialized = serializer.deserialize(csv)
        assertEquals("Buy milk, eggs", deserialized[0].title)
    }

    @Test
    fun `serialize handles quotes in fields by doubling`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Read \"War and Peace\"",
            description = "",
        )

        val csv = serializer.serialize(
            tasks = listOf(task),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        assertTrue(csv.contains("\"Read \"\"War and Peace\"\"\""))

        val deserialized = serializer.deserialize(csv)
        assertEquals("Read \"War and Peace\"", deserialized[0].title)
    }

    @Test
    fun `serialize handles newlines in fields by quoting`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Multi\nline\ntask",
            description = "",
        )

        val csv = serializer.serialize(
            tasks = listOf(task),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        val deserialized = serializer.deserialize(csv)
        assertEquals("Multi\nline\ntask", deserialized[0].title)
    }

    @Test
    fun `deserialize handles null values as empty strings`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Test",
            completedAt = null,
            dueAt = null,
            reminderOffsetMinutes = null,
            tag = null,
            archivedAt = null,
        )

        val csv = serializer.serialize(
            tasks = listOf(task),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        val deserialized = serializer.deserialize(csv)
        assertNull(deserialized[0].completedAt)
        assertNull(deserialized[0].dueAt)
        assertNull(deserialized[0].reminderOffsetMinutes)
        assertNull(deserialized[0].tag)
        assertNull(deserialized[0].archivedAt)
    }

    @Test
    fun `round trip with empty task list`() {
        val csv = serializer.serialize(
            tasks = emptyList(),
            schemaVersion = 1,
            exportedAt = 0L,
            appVersion = "1.0.0",
        )

        val deserialized = serializer.deserialize(csv)
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun `escapeCsvField does not quote simple strings`() {
        assertEquals("hello", CsvBackupSerializer.escapeCsvField("hello"))
        assertEquals("123", CsvBackupSerializer.escapeCsvField("123"))
    }

    @Test
    fun `escapeCsvField quotes strings with commas`() {
        assertEquals("\"a,b\"", CsvBackupSerializer.escapeCsvField("a,b"))
    }

    @Test
    fun `escapeCsvField quotes and doubles internal quotes`() {
        assertEquals("\"a\"\"b\"", CsvBackupSerializer.escapeCsvField("a\"b"))
    }

    @Test
    fun `escapeCsvField quotes strings with newlines`() {
        assertEquals("\"line1\nline2\"", CsvBackupSerializer.escapeCsvField("line1\nline2"))
    }

    @Test
    fun `parseCsvLines handles CRLF line endings`() {
        val csv = "a,b\r\nc,d\r\n"
        val result = CsvBackupSerializer.parseCsvLines(csv)
        assertEquals(2, result.size)
        assertEquals(listOf("a", "b"), result[0])
        assertEquals(listOf("c", "d"), result[1])
    }

    @Test
    fun `parseCsvLines handles mixed quoting`() {
        val csv = "\"quoted,field\",plain,\"has \"\"quotes\"\"\""
        val result = CsvBackupSerializer.parseCsvLines(csv)
        assertEquals(1, result.size)
        assertEquals(listOf("quoted,field", "plain", "has \"quotes\""), result[0])
    }
}
