package dev.tuandoan.tasktracker.domain.backup.model

/**
 * Result of an export operation.
 */
sealed class ExportResult {
    data class Success(val taskCount: Int) : ExportResult()
    data class Error(val message: String, val cause: Throwable? = null) : ExportResult()
}
