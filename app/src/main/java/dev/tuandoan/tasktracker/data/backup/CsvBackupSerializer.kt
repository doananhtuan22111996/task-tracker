package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import javax.inject.Inject

/**
 * CSV implementation of [BackupSerializer] following RFC 4180.
 * Used for export only (CSV import is not supported).
 * Metadata parameters (schemaVersion, exportedAt, appVersion) are ignored for CSV format.
 */
class CsvBackupSerializer @Inject constructor() : BackupSerializer {

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
        val fields = listOf(
            task.id.toString(),
            task.title,
            task.description,
            task.isCompleted.toString(),
            task.createdAt.toString(),
            task.completedAt?.toString() ?: "",
            task.dueAt?.toString() ?: "",
            task.reminderOffsetMinutes?.toString() ?: "",
            task.tag ?: "",
            task.isPinned.toString(),
            task.priority.toString(),
            task.isArchived.toString(),
            task.archivedAt?.toString() ?: "",
        )
        return fields.joinToString(",") { escapeCsvField(it) }
    }

    private fun parseCsvRowToDto(fields: List<String>): TaskBackupDto {
        if (fields.size < EXPECTED_FIELD_COUNT) {
            throw BackupParseException(
                "Expected $EXPECTED_FIELD_COUNT fields but got ${fields.size}",
            )
        }
        return TaskBackupDto(
            id = fields[0].toLong(),
            title = fields[1],
            description = fields[2],
            isCompleted = fields[3].toBooleanStrict(),
            createdAt = fields[4].toLong(),
            completedAt = fields[5].toLongOrNull(),
            dueAt = fields[6].toLongOrNull(),
            reminderOffsetMinutes = fields[7].toIntOrNull(),
            tag = fields[8].ifBlank { null },
            isPinned = fields[9].toBooleanStrict(),
            priority = fields[10].toInt(),
            isArchived = fields[11].toBooleanStrict(),
            archivedAt = fields[12].toLongOrNull(),
        )
    }

    companion object {
        private const val EXPECTED_FIELD_COUNT = 13

        private const val HEADER =
            "id,title,description,isCompleted,createdAt,completedAt,dueAt," +
                "reminderOffsetMinutes,tag,isPinned,priority,isArchived,archivedAt"

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
