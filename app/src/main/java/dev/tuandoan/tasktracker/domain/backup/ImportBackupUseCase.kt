package dev.tuandoan.tasktracker.domain.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupSerializer
import dev.tuandoan.tasktracker.di.JsonSerializer
import dev.tuandoan.tasktracker.domain.backup.model.ImportResult
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import dev.tuandoan.tasktracker.domain.usecase.SubtaskUseCase
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
    @ApplicationContext private val context: Context,
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
        val validTaskIds = validationResult.validTasks.map { it.id }.toSet()

        // Only keep subtasks whose parent task id survived validation; skip blank titles and
        // truncate to SubtaskUseCase.MAX_TITLE_LENGTH for consistency with the use case surface.
        val subtasks = dtos.flatMap { dto ->
            if (dto.id !in validTaskIds) return@flatMap emptyList()
            dto.subtasks.mapNotNull { sub ->
                val trimmed = sub.title.trim()
                if (trimmed.isEmpty()) return@mapNotNull null
                sub.toSubtask(taskId = dto.id)
                    .copy(title = trimmed.take(SubtaskUseCase.MAX_TITLE_LENGTH))
            }
        }

        repository.replaceAllTasksAndSubtasks(
            tasks = validationResult.validTasks,
            subtasks = subtasks,
        )

        ImportResult.Success(
            importedCount = validationResult.validTasks.size,
            skippedCount = validationResult.skippedCount,
        )
    } catch (e: Exception) {
        ImportResult.Error(
            message = context.getString(R.string.error_import_backup, e.message ?: ""),
            cause = e,
        )
    }
}
