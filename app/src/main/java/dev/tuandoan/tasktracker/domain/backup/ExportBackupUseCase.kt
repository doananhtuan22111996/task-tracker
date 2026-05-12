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
import dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent
import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.diagnostics.BackupEventFormat
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.diagnostics.bucketTaskCount
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
    private val analyticsLogger: AnalyticsLogger,
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
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export done count=${bucketTaskCount(tasks.size)}")
            // FB-14: fire only on success; the catch branch below is a separate funnel step.
            analyticsLogger.log(
                AnalyticsEvent.BackupExported(
                    format = format.toAnalytics(),
                    taskCount = tasks.size,
                ),
            )
            ExportResult.Success(taskCount = tasks.size)
        } catch (e: Exception) {
            // FB-12: no exception message — it can contain file paths or user-identifying strings.
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "export failed")
            // FB-14: no backup_exported event on failure; PRD has no "backup_export_failed"
            // counterpart. The breadcrumb captures the attempt for crash-report context.
            ExportResult.Error(
                message = context.getString(R.string.error_export_backup, e.message ?: ""),
                cause = e,
            )
        }
    }

    private fun BackupFormat.toAnalytics(): BackupEventFormat = when (this) {
        BackupFormat.JSON -> BackupEventFormat.JSON
        BackupFormat.CSV -> BackupEventFormat.CSV
    }
}
