package dev.tuandoan.tasktracker.data.backup

import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto

/**
 * Interface for serializing and deserializing task backup data.
 * Implementations handle specific formats (JSON, CSV).
 */
interface BackupSerializer {

    /**
     * Serializes a list of task DTOs along with metadata into a string.
     *
     * @param tasks The list of task DTOs to serialize.
     * @param schemaVersion The schema version of the backup format.
     * @param exportedAt The timestamp when the backup was created.
     * @param appVersion The app version string.
     * @return The serialized string representation of the backup.
     */
    fun serialize(tasks: List<TaskBackupDto>, schemaVersion: Int, exportedAt: Long, appVersion: String): String

    /**
     * Deserializes a string into a list of task DTOs.
     *
     * @param data The raw string content to deserialize.
     * @return The list of deserialized task DTOs.
     * @throws BackupParseException If the data cannot be parsed.
     */
    fun deserialize(data: String): List<TaskBackupDto>
}

/**
 * Exception thrown when backup data cannot be parsed.
 */
class BackupParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
