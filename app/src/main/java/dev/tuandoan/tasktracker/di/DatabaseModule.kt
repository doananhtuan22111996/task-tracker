package dev.tuandoan.tasktracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.tasktracker.data.database.TaskDao
import dev.tuandoan.tasktracker.data.database.TaskDatabase
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 * Uses @Provides for complex object creation that requires custom logic.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Helper function to check if a column exists in a table (shared across migrations)
     */
    private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameColumnIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                val existingColumnName = cursor.getString(nameColumnIndex)
                if (existingColumnName == columnName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Migration from version 1 to 2: Add due date and reminder columns (idempotent)
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add dueAt column only if it doesn't exist (nullable Long for epoch millis)
            if (!hasColumn(database, "tasks", "dueAt")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN dueAt INTEGER")
            }

            // Add reminderOffsetMinutes column only if it doesn't exist (nullable Int for offset in minutes)
            if (!hasColumn(database, "tasks", "reminderOffsetMinutes")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN reminderOffsetMinutes INTEGER")
            }
        }
    }

    /**
     * Migration from version 2 to 3: Add tag column (idempotent)
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add tag column only if it doesn't exist (nullable String for single tag)
            if (!hasColumn(database, "tasks", "tag")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN tag TEXT")
            }
        }
    }

    /**
     * Migration from version 3 to 4: Add pin and priority columns (idempotent)
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add isPinned column only if it doesn't exist (Boolean with default false)
            if (!hasColumn(database, "tasks", "isPinned")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }

            // Add priority column only if it doesn't exist (Int with default 1 = MEDIUM)
            if (!hasColumn(database, "tasks", "priority")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 1")
            }
        }
    }

    /**
     * Migration from version 4 to 5: Add archive columns (idempotent)
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add isArchived column only if it doesn't exist (Boolean with default false)
            if (!hasColumn(database, "tasks", "isArchived")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }

            // Add archivedAt column only if it doesn't exist (nullable Long for timestamp)
            if (!hasColumn(database, "tasks", "archivedAt")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN archivedAt INTEGER")
            }
        }
    }

    /**
     * Migration from version 5 to 6: Add completedAt column (idempotent)
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add completedAt column only if it doesn't exist (nullable Long for completion timestamp)
            if (!hasColumn(database, "tasks", "completedAt")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
            }
        }
    }

    /**
     * Migration from version 6 to 7: Add dueAtHasTime column (idempotent)
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            if (!hasColumn(database, "tasks", "dueAtHasTime")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN dueAtHasTime INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    /**
     * Migration from version 7 to 8: Add recurrence columns (idempotent)
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            if (!hasColumn(database, "tasks", "recurrenceType")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceType INTEGER NOT NULL DEFAULT 0")
            }
            if (!hasColumn(database, "tasks", "recurrenceInterval")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
            }
            if (!hasColumn(database, "tasks", "recurrenceDaysOfWeek")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceDaysOfWeek INTEGER NOT NULL DEFAULT 0")
            }
            if (!hasColumn(database, "tasks", "recurrenceEndDate")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceEndDate INTEGER")
            }
            if (!hasColumn(database, "tasks", "parentRecurringTaskId")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN parentRecurringTaskId INTEGER")
            }
        }
    }

    /**
     * Migration from version 8 to 9: Add indexes for query performance (idempotent)
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted_isArchived` ON `tasks` (`isCompleted`, `isArchived`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_parentRecurringTaskId` ON `tasks` (`parentRecurringTaskId`)",
            )
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            if (!hasColumn(database, "tasks", "tagColor")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN tagColor TEXT")
            }
        }
    }

    /**
     * Provides a singleton instance of TaskDatabase.
     * Uses Room.databaseBuilder for database creation with proper configuration.
     */
    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase = Room.databaseBuilder(
        context = context.applicationContext,
        klass = TaskDatabase::class.java,
        name = "task_database",
    )
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
        )
        .build()

    /**
     * Provides TaskDao from the database.
     * No @Singleton needed here as it's tied to the singleton database instance.
     */
    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()
}
