package dev.tuandoan.tasktracker.domain.backup

import dev.tuandoan.tasktracker.data.database.Task

/**
 * Interface for validating and sanitizing imported task data.
 */
interface BackupValidator {

    /**
     * Validates and sanitizes a list of tasks. Invalid tasks may be corrected or skipped.
     *
     * @param tasks The raw list of tasks parsed from a backup file.
     * @return A [ValidationResult] containing the valid tasks and the count of skipped tasks.
     */
    fun validate(tasks: List<Task>): ValidationResult

    /**
     * Result of validation containing the sanitized tasks and count of skipped entries.
     */
    data class ValidationResult(val validTasks: List<Task>, val skippedCount: Int)
}
