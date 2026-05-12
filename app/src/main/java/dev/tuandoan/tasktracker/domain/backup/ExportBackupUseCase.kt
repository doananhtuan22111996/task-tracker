package dev.tuandoan.tasktracker.domain.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupSerializer
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.di.CsvSerializer
import dev.tuandoan.tasktracker.di.JsonSerializer
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.domain.backup.model.BackupFormat
import dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata
import dev.tuandoan.tasktracker.domain.backup.model.ExportResult
import dev.tuandoan.tasktracker.domain.repository.ISubtaskRepository
import dev.tuandoan.tasktracker.domain.repository.ITaskRepository
import javax.inject.Inject

/**
 * Use case that orchestrates the export of all tasks to a backup file.
 */
class ExportBackupUseCase @Inject constructor(
    private val repository: ITaskRepository,
    private val subtaskRepository: ISubtaskRepository,
    @JsonSerializer private val jsonSerializer: BackupSerializer,
    @CsvSerializer private val csvSerializer: BackupSerializer,
    private val fileProvider: BackupFileProvider,
    @ApplicationContext private val context: Context,
    private val breadcrumbLogger: BreadcrumbLogger,
) {

    /**
     * Exports all tasks (including archived) to the specified URI in the given format.
     *
     * @param uri The SAF URI to write the backup file to.
     * @param format The backup format (JSON or CSV).
     * @param appVersion The current app version string.
     * @return An [ExportResult] indicating success or failure.
     */
    suspend fun execute(uri: Uri, format: BackupFormat, appVersion: String): ExportResult {
        // FB-12: format name is the enum constant ("JSON" / "CSV"), not user content.
        breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export start format=${format.name}")
        return try {
            val tasks = repository.getAllTasksIncludingArchived()
            val allSubtasks = subtaskRepository.getAllSubtasks()
            val subtasksByTask = allSubtasks.groupBy { it.taskId }
            val dtos = tasks.map { task ->
                TaskBackupDto.fromTask(task, subtasksByTask[task.id].orEmpty())
            }
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

            // FB-12: bucketed count — same privacy rule as CrashlyticsKeysWriter.task_count_bucket.
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export done count=${bucket(tasks.size)}")
            ExportResult.Success(taskCount = tasks.size)
        } catch (e: Exception) {
            // FB-12: no exception message — it can contain file paths or user-identifying strings.
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export failed")
            ExportResult.Error(
                message = context.getString(R.string.error_export_backup, e.message ?: ""),
                cause = e,
            )
        }
    }

    private fun bucket(count: Int): String = when {
        count <= 0 -> "0"
        count < 10 -> "1-9"
        count < 50 -> "10-49"
        count < 200 -> "50-199"
        else -> "200+"
    }
}
