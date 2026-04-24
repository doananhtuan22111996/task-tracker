package dev.tuandoan.tasktracker.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 11 to 12: Add the `subtasks` table for checklist items per task.
 *
 * Schema matches the [Subtask] entity:
 * - FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE
 * - Index on taskId for efficient lookup by parent task
 *
 * Idempotent: uses CREATE TABLE IF NOT EXISTS and CREATE INDEX IF NOT EXISTS so re-running
 * the migration (e.g., on a partially-migrated DB) is safe.
 */
internal val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `subtasks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`taskId` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`isCompleted` INTEGER NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)",
        )
    }
}
