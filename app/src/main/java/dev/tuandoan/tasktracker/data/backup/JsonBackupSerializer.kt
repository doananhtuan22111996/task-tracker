package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.BackupPayload
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * JSON implementation of [BackupSerializer] using kotlinx.serialization.
 * Produces and consumes the full JSON envelope format with metadata.
 */
class JsonBackupSerializer @Inject constructor() : BackupSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun serialize(
        tasks: List<TaskBackupDto>,
        schemaVersion: Int,
        exportedAt: Long,
        appVersion: String,
    ): String {
        val payload = BackupPayload(
            schemaVersion = schemaVersion,
            exportedAt = exportedAt,
            appVersion = appVersion,
            taskCount = tasks.size,
            tasks = tasks,
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    override fun deserialize(data: String): List<TaskBackupDto> {
        try {
            val payload = json.decodeFromString(BackupPayload.serializer(), data)
            if (payload.schemaVersion > BackupMetadata.CURRENT_SCHEMA_VERSION) {
                throw BackupParseException(
                    "This backup was created by a newer version of Task Tracker " +
                        "(schema v${payload.schemaVersion}). Please update the app and try again.",
                )
            }
            return payload.tasks
        } catch (e: BackupParseException) {
            throw e
        } catch (e: Exception) {
            throw BackupParseException("Failed to parse JSON backup: ${e.message}", e)
        }
    }
}
