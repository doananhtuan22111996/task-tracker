package dev.tuandoan.tasktracker.domain.backup

import android.net.Uri
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupSerializer
import dev.tuandoan.tasktracker.di.JsonSerializer
import dev.tuandoan.tasktracker.domain.backup.model.ImportResult
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Use case that orchestrates the import of tasks from a backup file.
 * Import replaces all existing tasks (full restore).
 */
class ImportBackupUseCase @Inject constructor(
    private val repository: ITaskRepository,
    @JsonSerializer private val jsonSerializer: BackupSerializer,
    private val fileProvider: BackupFileProvider,
    private val validator: BackupValidator,
) {

    /**
     * Imports tasks from the specified URI, replacing all existing tasks.
     *
     * @param uri The SAF URI of the backup file to import.
     * @return An [ImportResult] indicating success or failure.
     */
    suspend fun execute(uri: Uri): ImportResult = try {
        val rawContent = fileProvider.readFromUri(uri)
        val dtos = jsonSerializer.deserialize(rawContent)
        val tasks = dtos.map { it.toTask() }

        val validationResult = validator.validate(tasks)

        repository.replaceAllTasks(validationResult.validTasks)

        ImportResult.Success(
            importedCount = validationResult.validTasks.size,
            skippedCount = validationResult.skippedCount,
        )
    } catch (e: Exception) {
        ImportResult.Error(
            message = "Failed to import backup: ${e.message}",
            cause = e,
        )
    }
}
