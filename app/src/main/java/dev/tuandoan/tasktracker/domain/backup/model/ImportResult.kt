package dev.tuandoan.tasktracker.domain.backup.model

/**
 * Result of an import operation.
 */
sealed class ImportResult {
    data class Success(val importedCount: Int, val skippedCount: Int) : ImportResult()

    data class Error(val message: String, val cause: Throwable? = null) : ImportResult()
}
