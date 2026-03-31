package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.data.database.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceBackupTest {

    private val csvSerializer = CsvBackupSerializer()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // === CSV Tests ===

    @Test
    fun `csv round trip preserves all recurrence fields`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Weekly standup",
            recurrenceType = 2,
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = 42, // Mon+Wed+Fri = 1+4+16 = 21... actually Mon=1,Wed=4,Fri=16 = 21
            recurrenceEndDate = 1700000000000L,
            parentRecurringTaskId = 99L,
        )

        val csv = csvSerializer.serialize(listOf(task), 2, 0L, "1.4.0")
        val deserialized = csvSerializer.deserialize(csv)

        assertEquals(1, deserialized.size)
        assertEquals(2, deserialized[0].recurrenceType)
        assertEquals(1, deserialized[0].recurrenceInterval)
        assertEquals(42, deserialized[0].recurrenceDaysOfWeek)
        assertEquals(1700000000000L, deserialized[0].recurrenceEndDate)
        assertEquals(99L, deserialized[0].parentRecurringTaskId)
    }

    @Test
    fun `csv round trip preserves null recurrence optional fields`() {
        val task = TaskBackupDto(
            id = 1L,
            title = "Daily task",
            recurrenceType = 1,
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = 0,
            recurrenceEndDate = null,
            parentRecurringTaskId = null,
        )

        val csv = csvSerializer.serialize(listOf(task), 2, 0L, "1.4.0")
        val deserialized = csvSerializer.deserialize(csv)

        assertEquals(1, deserialized.size)
        assertEquals(1, deserialized[0].recurrenceType)
        assertNull(deserialized[0].recurrenceEndDate)
        assertNull(deserialized[0].parentRecurringTaskId)
    }

    @Test
    fun `csv import of v1 14-column format defaults recurrence fields`() {
        val v1Csv = """
            id,title,description,isCompleted,createdAt,completedAt,dueAt,dueAtHasTime,reminderOffsetMinutes,tag,isPinned,priority,isArchived,archivedAt
            1,Task,,false,1700000000000,,1700100000000,false,,,false,1,false,
        """.trimIndent()

        val deserialized = csvSerializer.deserialize(v1Csv)

        assertEquals(1, deserialized.size)
        assertEquals(0, deserialized[0].recurrenceType)
        assertEquals(1, deserialized[0].recurrenceInterval)
        assertEquals(0, deserialized[0].recurrenceDaysOfWeek)
        assertNull(deserialized[0].recurrenceEndDate)
        assertNull(deserialized[0].parentRecurringTaskId)
    }

    @Test
    fun `csv header includes recurrence columns`() {
        val csv = csvSerializer.serialize(
            listOf(TaskBackupDto(id = 1L, title = "Test")),
            2,
            0L,
            "1.4.0",
        )

        val header = csv.lines().first()
        assertTrue(header.contains("recurrenceType"))
        assertTrue(header.contains("recurrenceInterval"))
        assertTrue(header.contains("recurrenceDaysOfWeek"))
        assertTrue(header.contains("recurrenceEndDate"))
        assertTrue(header.contains("parentRecurringTaskId"))
    }

    // === JSON Tests ===

    @Test
    fun `json round trip preserves all recurrence fields`() {
        val dto = TaskBackupDto(
            id = 1L,
            title = "Weekly standup",
            recurrenceType = 2,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = 21,
            recurrenceEndDate = 1700000000000L,
            parentRecurringTaskId = 99L,
        )

        val jsonStr = json.encodeToString(dto)
        val deserialized = json.decodeFromString<TaskBackupDto>(jsonStr)

        assertEquals(2, deserialized.recurrenceType)
        assertEquals(2, deserialized.recurrenceInterval)
        assertEquals(21, deserialized.recurrenceDaysOfWeek)
        assertEquals(1700000000000L, deserialized.recurrenceEndDate)
        assertEquals(99L, deserialized.parentRecurringTaskId)
    }

    @Test
    fun `json import without recurrence fields defaults correctly`() {
        val v1Json = """
            {"id":1,"title":"Task","description":"","isCompleted":false,"createdAt":0,"completedAt":null,"dueAt":null,"dueAtHasTime":false,"reminderOffsetMinutes":null,"tag":null,"isPinned":false,"priority":1,"isArchived":false,"archivedAt":null}
        """.trimIndent()

        val deserialized = json.decodeFromString<TaskBackupDto>(v1Json)

        assertEquals(0, deserialized.recurrenceType)
        assertEquals(1, deserialized.recurrenceInterval)
        assertEquals(0, deserialized.recurrenceDaysOfWeek)
        assertNull(deserialized.recurrenceEndDate)
        assertNull(deserialized.parentRecurringTaskId)
    }

    // === DTO Mapping Tests ===

    @Test
    fun `fromTask maps recurrence fields correctly`() {
        val task = Task(
            id = 1L,
            title = "Recurring",
            recurrenceType = 2,
            recurrenceInterval = 3,
            recurrenceDaysOfWeek = 21,
            recurrenceEndDate = 1700000000000L,
            parentRecurringTaskId = 50L,
        )

        val dto = TaskBackupDto.fromTask(task)

        assertEquals(2, dto.recurrenceType)
        assertEquals(3, dto.recurrenceInterval)
        assertEquals(21, dto.recurrenceDaysOfWeek)
        assertEquals(1700000000000L, dto.recurrenceEndDate)
        assertEquals(50L, dto.parentRecurringTaskId)
    }

    @Test
    fun `toTask maps recurrence fields correctly`() {
        val dto = TaskBackupDto(
            id = 1L,
            title = "Recurring",
            recurrenceType = 2,
            recurrenceInterval = 3,
            recurrenceDaysOfWeek = 21,
            recurrenceEndDate = 1700000000000L,
            parentRecurringTaskId = 50L,
        )

        val task = dto.toTask()

        assertEquals(2, task.recurrenceType)
        assertEquals(3, task.recurrenceInterval)
        assertEquals(21, task.recurrenceDaysOfWeek)
        assertEquals(1700000000000L, task.recurrenceEndDate)
        assertEquals(50L, task.parentRecurringTaskId)
    }

    @Test
    fun `fromTask then toTask round trip preserves recurrence fields`() {
        val original = Task(
            id = 99L,
            title = "Round Trip Recurring",
            recurrenceType = 3,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = 127,
            recurrenceEndDate = 1700000000000L,
            parentRecurringTaskId = 42L,
        )

        val roundTripped = TaskBackupDto.fromTask(original).toTask()

        assertEquals(original.recurrenceType, roundTripped.recurrenceType)
        assertEquals(original.recurrenceInterval, roundTripped.recurrenceInterval)
        assertEquals(original.recurrenceDaysOfWeek, roundTripped.recurrenceDaysOfWeek)
        assertEquals(original.recurrenceEndDate, roundTripped.recurrenceEndDate)
        assertEquals(original.parentRecurringTaskId, roundTripped.parentRecurringTaskId)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
