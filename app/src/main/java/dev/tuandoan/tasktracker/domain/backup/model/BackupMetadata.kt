package dev.tuandoan.tasktracker.domain.backup.model

/**
 * Metadata included in backup files for versioning and traceability.
 */
data class BackupMetadata(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
