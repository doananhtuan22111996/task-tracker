package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.SubtaskBackupDto
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * CSV implementation of [BackupSerializer] following RFC 4180.
 * Used for export only (CSV import is not supported).
 * Metadata parameters (schemaVersion, exportedAt, appVersion) are ignored for CSV format.
 */
class CsvBackupSerializer @Inject constructor() : BackupSerializer {

    private val subtaskListJson = Json { ignoreUnknownKeys = true }

    override fun serialize(
        tasks: List<TaskBackupDto>,
        schemaVersion: Int,
        exportedAt: Long,
        appVersion: String,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        for (task in tasks) {
            sb.appendLine(taskToCsvRow(task))
        }
        return sb.toString()
    }

    override fun deserialize(data: String): List<TaskBackupDto> {
        val lines = parseCsvLines(data)
        if (lines.isEmpty()) {
            return emptyList()
        }

        // Skip header row
        val dataLines = if (lines.first().firstOrNull()?.equals("id", ignoreCase = true) == true) {
            lines.drop(1)
        } else {
            lines
        }

        return dataLines.mapNotNull { fields ->
            try {
                parseCsvRowToDto(fields)
            } catch (e: Exception) {
                throw BackupParseException("Failed to parse CSV row: ${e.message}", e)
            }
        }
    }

    private fun taskToCsvRow(task: TaskBackupDto): String {
        val subtasksJson = if (task.subtasks.isEmpty()) {
            ""
        } else {
            subtaskListJson.encodeToString(ListSerializer(SubtaskBackupDto.serializer()), task.subtasks)
        }
        val fields = listOf(
            task.id.toString(),
            task.title,
            task.description,
            task.isCompleted.toString(),
            task.createdAt.toString(),
            task.completedAt?.toString() ?: "",
            task.dueAt?.toString() ?: "",
            task.dueAtHasTime.toString(),
            task.reminderOffsetMinutes?.toString() ?: "",
            task.tag ?: "",
            task.tagColor ?: "",
            task.isPinned.toString(),
            task.priority.toString(),
            task.isArchived.toString(),
            task.archivedAt?.toString() ?: "",
            task.recurrenceType.toString(),
            task.recurrenceInterval.toString(),
            task.recurrenceDaysOfWeek.toString(),
            task.recurrenceEndDate?.toString() ?: "",
            task.parentRecurringTaskId?.toString() ?: "",
            subtasksJson,
        )
        return fields.joinToString(",") { escapeCsvField(it) }
    }

    private fun parseCsvRowToDto(fields: List<String>): TaskBackupDto {
        if (fields.size < LEGACY_FIELD_COUNT) {
            throw BackupParseException(
                "Expected at least $LEGACY_FIELD_COUNT fields but got ${fields.size}",
            )
        }

        // Support legacy 13-column, v1 14-column, v2 19-column, v2-color 20-column, and v3 21-column formats
        val hasDueAtHasTime = fields.size >= V1_FIELD_COUNT
        val offset = if (hasDueAtHasTime) 1 else 0
        val hasTagColor = fields.size >= V2_COLOR_FIELD_COUNT
        val colorOffset = if (hasTagColor) 1 else 0
        val hasRecurrence = fields.size >= (V2_FIELD_COUNT + offset)
        val hasSubtasks = fields.size >= EXPECTED_FIELD_COUNT

        val subtasksJson = if (hasSubtasks) fields[18 + offset + colorOffset] else ""
        val subtasks = if (subtasksJson.isBlank()) {
            emptyList()
        } else {
            try {
                subtaskListJson.decodeFromString(ListSerializer(SubtaskBackupDto.serializer()), subtasksJson)
            } catch (e: Exception) {
                throw BackupParseException("Failed to parse subtasks cell: ${e.message}", e)
            }
        }

        return TaskBackupDto(
            id = fields[0].toLong(),
            title = fields[1],
            description = fields[2],
            isCompleted = fields[3].toBooleanStrict(),
            createdAt = fields[4].toLong(),
            completedAt = fields[5].toLongOrNull(),
            dueAt = fields[6].toLongOrNull(),
            dueAtHasTime = if (hasDueAtHasTime) fields[7].toBooleanStrictOrNull() ?: false else false,
            reminderOffsetMinutes = fields[7 + offset].toIntOrNull(),
            tag = fields[8 + offset].ifBlank { null },
            tagColor = if (hasTagColor) fields[9 + offset].ifBlank { null } else null,
            isPinned = fields[9 + offset + colorOffset].toBooleanStrict(),
            priority = fields[10 + offset + colorOffset].toInt(),
            isArchived = fields[11 + offset + colorOffset].toBooleanStrict(),
            archivedAt = fields[12 + offset + colorOffset].toLongOrNull(),
            recurrenceType = if (hasRecurrence) fields[13 + offset + colorOffset].toIntOrNull() ?: 0 else 0,
            recurrenceInterval = if (hasRecurrence) fields[14 + offset + colorOffset].toIntOrNull() ?: 1 else 1,
            recurrenceDaysOfWeek = if (hasRecurrence) fields[15 + offset + colorOffset].toIntOrNull() ?: 0 else 0,
            recurrenceEndDate = if (hasRecurrence) fields[16 + offset + colorOffset].toLongOrNull() else null,
            parentRecurringTaskId = if (hasRecurrence) fields[17 + offset + colorOffset].toLongOrNull() else null,
            subtasks = subtasks,
        )
    }

    companion object {
        private const val EXPECTED_FIELD_COUNT = 21
        private const val V2_COLOR_FIELD_COUNT = 20
        private const val V2_FIELD_COUNT = 19
        private const val V1_FIELD_COUNT = 14
        private const val LEGACY_FIELD_COUNT = 13

        private const val HEADER =
            "id,title,description,isCompleted,createdAt,completedAt,dueAt,dueAtHasTime," +
                "reminderOffsetMinutes,tag,tagColor,isPinned,priority,isArchived,archivedAt," +
                "recurrenceType,recurrenceInterval,recurrenceDaysOfWeek,recurrenceEndDate," +
                "parentRecurringTaskId,subtasks"

        /**
         * Escapes a CSV field per RFC 4180: wrap in quotes if the field contains
         * a comma, double-quote, or newline. Internal double-quotes are doubled.
         */
        fun escapeCsvField(field: String): String =
            if (field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')) {
                "\"${field.replace("\"", "\"\"")}\""
            } else {
                field
            }

        /**
         * Parses a CSV string into rows of fields, handling quoted fields per RFC 4180.
         */
        fun parseCsvLines(data: String): List<List<String>> {
            val rows = mutableListOf<List<String>>()
            val currentField = StringBuilder()
            val currentRow = mutableListOf<String>()
            var inQuotes = false
            var i = 0

            while (i < data.length) {
                val c = data[i]
                when {
                    inQuotes -> {
                        if (c == '"') {
                            // Check for escaped quote
                            if (i + 1 < data.length && data[i + 1] == '"') {
                                currentField.append('"')
                                i += 2
                                continue
                            } else {
                                inQuotes = false
                                i++
                                continue
                            }
                        } else {
                            currentField.append(c)
                        }
                    }
                    c == '"' -> {
                        inQuotes = true
                    }
                    c == ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    c == '\r' -> {
                        // Handle \r\n or standalone \r
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                        if (i + 1 < data.length && data[i + 1] == '\n') {
                            i++
                        }
                    }
                    c == '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
                i++
            }

            // Handle last field/row
            if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
                currentRow.add(currentField.toString())
                rows.add(currentRow.toList())
            }

            return rows
        }
    }
}
