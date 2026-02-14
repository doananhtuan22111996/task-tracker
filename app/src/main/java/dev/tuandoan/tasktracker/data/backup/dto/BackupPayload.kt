package dev.tuandoan.tasktracker.data.backup.dto

import kotlinx.serialization.Serializable

/**
 * JSON envelope for backup files. Contains metadata and the list of task DTOs.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersion: String,
    val taskCount: Int,
    val tasks: List<TaskBackupDto>,
)
