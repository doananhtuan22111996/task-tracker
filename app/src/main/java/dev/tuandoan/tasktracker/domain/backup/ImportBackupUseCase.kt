package dev.tuandoan.tasktracker.domain.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.data.backup.BackupFileProvider
import dev.tuandoan.tasktracker.data.backup.BackupSerializer
import dev.tuandoan.tasktracker.di.JsonSerializer
import dev.tuandoan.tasktracker.diagnostics.AnalyticsEvent
import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.diagnostics.BackupEventFormat
import dev.tuandoan.tasktracker.diagnostics.BackupOutcome
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbCategory
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.diagnostics.PerformanceLogger
import dev.tuandoan.tasktracker.diagnostics.PerformanceTraceName
import dev.tuandoan.tasktracker.diagnostics.bucketTaskCount
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
    private val breadcrumbLogger: BreadcrumbLogger,
    private val analyticsLogger: AnalyticsLogger,
    private val performanceLogger: PerformanceLogger,
) {

    /**
     * Imports tasks from the specified URI, replacing all existing tasks.
     *
     * @param uri The SAF URI of the backup file to import.
     * @return An [ImportResult] indicating success or failure.
     */
    suspend fun execute(uri: Uri): ImportResult {
        // FB-17: `backup_import` Performance trace — measures end-to-end import duration
        // (start before any other work → finally after DB replace). `record_count`
        // attribute carries the bucketed valid-count so the Firebase Performance console
        // can slice latency by import size without leaking exact counts. Set in `finally`
        // so the attribute reflects the final value on both success and error paths.
        // Started BEFORE the breadcrumb so the trace literally measures the whole call.
        val trace = performanceLogger.start(PerformanceTraceName.BackupImport)
        var recordCountForTrace = 0
        // FB-12: no uri, no file content — just a "we started" marker.
        breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "import start")
        try {
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

            // FB-12: bucketed counts only.
            val validCount = validationResult.validTasks.size
            val skippedCount = validationResult.skippedCount
            recordCountForTrace = validCount
            breadcrumbLogger.log(
                BreadcrumbCategory.BACKUP,
                "import done count=${bucketTaskCount(validCount)} skipped=${bucketTaskCount(skippedCount)}",
            )
            // FB-14: `outcome` is partial when validation dropped rows, success otherwise.
            // Failure path (below) maps to BackupOutcome.ERROR with the raw exception message
            // deliberately omitted (same rule as the breadcrumb).
            val outcome = if (skippedCount > 0) BackupOutcome.PARTIAL else BackupOutcome.SUCCESS
            analyticsLogger.log(
                AnalyticsEvent.BackupImported(
                    format = BackupEventFormat.JSON,
                    recordCount = validCount,
                    outcome = outcome,
                ),
            )

            return ImportResult.Success(
                importedCount = validationResult.validTasks.size,
                skippedCount = validationResult.skippedCount,
            )
        } catch (e: Exception) {
            // FB-12: no exception message — may contain file paths.
            breadcrumbLogger.log(BreadcrumbCategory.BACKUP, "import failed")
            // FB-14: outcome=ERROR captures the failure in the funnel without the exception
            // message (which may contain file paths). record_count=0 matches the zero-rows-
            // imported reality.
            analyticsLogger.log(
                AnalyticsEvent.BackupImported(
                    format = BackupEventFormat.JSON,
                    recordCount = 0,
                    outcome = BackupOutcome.ERROR,
                ),
            )
            return ImportResult.Error(
                message = context.getString(R.string.error_import_backup, e.message ?: ""),
                cause = e,
            )
        } finally {
            // FB-17: `record_count` is bucketed to mirror the FB-14 Analytics event taxonomy
            // (avoids raw-count re-identification at the extremes). On error path, value is
            // `0` which matches the zero-rows-imported reality.
            trace.putAttribute(ATTR_RECORD_COUNT, bucketTaskCount(recordCountForTrace))
            trace.stop()
        }
    }

    private companion object {
        // Firebase Performance attribute keys are limited to 40 chars; this is well within.
        const val ATTR_RECORD_COUNT = "record_count"
    }
}
