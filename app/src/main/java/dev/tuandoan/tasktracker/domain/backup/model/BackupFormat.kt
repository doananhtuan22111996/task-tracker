package dev.tuandoan.tasktracker.domain.backup.model

/**
 * Supported backup file formats.
 */
enum class BackupFormat(val mimeType: String, val extension: String) {
    JSON("application/json", "json"),
    CSV("text/csv", "csv"),
}
