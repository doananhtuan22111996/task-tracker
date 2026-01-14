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
     * Migration from version 1 to 2: Add due date and reminder columns (idempotent)
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Helper function to check if a column exists in a table
            fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
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
     * Provides a singleton instance of TaskDatabase.
     * Uses Room.databaseBuilder for database creation with proper configuration.
     */
    @Provides
    @Singleton
    fun provideTaskDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase {
        return Room.databaseBuilder(
            context = context.applicationContext,
            klass = TaskDatabase::class.java,
            name = "task_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    /**
     * Provides TaskDao from the database.
     * No @Singleton needed here as it's tied to the singleton database instance.
     */
    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao {
        return database.taskDao()
    }
}