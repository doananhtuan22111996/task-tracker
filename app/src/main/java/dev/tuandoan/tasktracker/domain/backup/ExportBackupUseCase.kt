package dev.tuandoan.tasktracker.domain.backup

import android.net.Uri
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupSerializer
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.di.CsvSerializer
import dev.tuandoan.tasktracker.di.JsonSerializer
import dev.tuandoan.tasktracker.domain.backup.model.BackupFormat
import dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata
import dev.tuandoan.tasktracker.domain.backup.model.ExportResult
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Use case that orchestrates the export of all tasks to a backup file.
 */
class ExportBackupUseCase @Inject constructor(
    private val repository: ITaskRepository,
    @JsonSerializer private val jsonSerializer: BackupSerializer,
    @CsvSerializer private val csvSerializer: BackupSerializer,
    private val fileProvider: BackupFileProvider,
) {

    /**
     * Exports all tasks (including archived) to the specified URI in the given format.
     *
     * @param uri The SAF URI to write the backup file to.
     * @param format The backup format (JSON or CSV).
     * @param appVersion The current app version string.
     * @return An [ExportResult] indicating success or failure.
     */
    suspend fun execute(uri: Uri, format: BackupFormat, appVersion: String): ExportResult = try {
        val tasks = repository.getAllTasksIncludingArchived()
        val dtos = tasks.map { TaskBackupDto.fromTask(it) }
        val metadata = BackupMetadata(appVersion = appVersion)

        val serializer = when (format) {
            BackupFormat.JSON -> jsonSerializer
            BackupFormat.CSV -> csvSerializer
        }

        val content = serializer.serialize(
            tasks = dtos,
            schemaVersion = metadata.schemaVersion,
            exportedAt = metadata.exportedAt,
            appVersion = metadata.appVersion,
        )

        fileProvider.writeToUri(uri, content)

        ExportResult.Success(taskCount = tasks.size)
    } catch (e: Exception) {
        ExportResult.Error(
            message = "Failed to export backup: ${e.message}",
            cause = e,
        )
    }
}
