package dev.tuandoan.tasktracker.data.database.migration

import android.content.ContentValues
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tuandoan.tasktracker.data.database.TaskDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TaskDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate_11_to_12_preservesTaskData() {
        helper.createDatabase(TEST_DB_NAME, 11).use { db ->
            db.insert("tasks", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, v11TaskValues(id = 1))
            db.insert(
                "tasks",
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                v11TaskValues(id = 2, title = "Second"),
            )
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            12,
            /* validateDroppedTables = */
            true,
            MIGRATION_11_12,
        )

        migratedDb.query("SELECT id, title FROM tasks ORDER BY id ASC").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("First", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals(2L, cursor.getLong(0))
            assertEquals("Second", cursor.getString(1))
        }
    }

    @Test
    fun migrate_11_to_12_createsSubtasksTable() {
        helper.createDatabase(TEST_DB_NAME, 11).close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            12,
            true,
            MIGRATION_11_12,
        )

        // Validate the table exists by reading sqlite_master.
        migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'subtasks'",
        ).use { cursor ->
            assertEquals(1, cursor.count)
        }

        // Validate the FK index exists.
        migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_subtasks_taskId'",
        ).use { cursor ->
            assertEquals(1, cursor.count)
        }

        // Validate column layout.
        val expectedColumns = setOf("id", "taskId", "title", "isCompleted", "sortOrder", "createdAt")
        val actualColumns = mutableSetOf<String>()
        migratedDb.query("PRAGMA table_info(subtasks)").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                actualColumns += cursor.getString(nameIdx)
            }
        }
        assertEquals(expectedColumns, actualColumns)
    }

    @Test
    fun migrate_11_to_12_cascadeDeleteRemovesSubtasks() {
        helper.createDatabase(TEST_DB_NAME, 11).use { db ->
            db.insert("tasks", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, v11TaskValues(id = 1))
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 12, true, MIGRATION_11_12)

        // Open the fully-migrated DB via Room so foreign keys are enforced (Room enables FKs by default).
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, TaskDatabase::class.java, TEST_DB_NAME)
            .addMigrations(MIGRATION_11_12)
            .build()

        try {
            db.openHelper.writableDatabase.use { sqlite ->
                val subtaskValues = ContentValues().apply {
                    put("taskId", 1L)
                    put("title", "Buy milk")
                    put("isCompleted", 0)
                    put("sortOrder", 0)
                    put("createdAt", 1_705_320_000_000L)
                }
                val subtaskId = sqlite.insert(
                    "subtasks",
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                    subtaskValues,
                )
                assertNotNull(subtaskId)
                assertTrue(subtaskId > 0)

                sqlite.query("SELECT COUNT(*) FROM subtasks WHERE taskId = 1").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(1, cursor.getInt(0))
                }

                // Delete the parent task. The FK cascade should remove the child subtask.
                sqlite.delete("tasks", "id = ?", arrayOf("1"))

                sqlite.query("SELECT COUNT(*) FROM subtasks WHERE taskId = 1").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                }
            }
        } finally {
            db.close()
        }
    }

    private fun v11TaskValues(id: Long, title: String = "First"): ContentValues = ContentValues().apply {
        put("id", id)
        put("title", title)
        put("description", "")
        put("isCompleted", 0)
        put("createdAt", 1_705_320_000_000L)
        putNull("completedAt")
        putNull("dueAt")
        put("dueAtHasTime", 0)
        putNull("reminderOffsetMinutes")
        putNull("tag")
        putNull("tagColor")
        put("isPinned", 0)
        put("priority", 1)
        put("isArchived", 0)
        putNull("archivedAt")
        put("recurrenceType", 0)
        put("recurrenceInterval", 1)
        put("recurrenceDaysOfWeek", 0)
        putNull("recurrenceEndDate")
        putNull("parentRecurringTaskId")
    }

    companion object {
        private const val TEST_DB_NAME = "task_database_migration_test"
    }
}
