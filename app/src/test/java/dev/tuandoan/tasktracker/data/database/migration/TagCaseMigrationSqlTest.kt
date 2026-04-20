package dev.tuandoan.tasktracker.data.database.migration

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Executes the migration v10 → v11 SQL against a real in-memory SQLite database (JDBC).
 *
 * This is a JVM-only test that validates the exact SQL the app runs in production, without
 * needing AndroidTest infrastructure or Room's MigrationTestHelper. The schema is a minimal
 * subset matching the columns the migration touches — only `tag`, `tagColor`, and `createdAt`
 * (for tie-breaking) are relevant.
 */
class TagCaseMigrationSqlTest {

    private lateinit var conn: Connection

    @Before
    fun setup() {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE tasks (
                    id INTEGER PRIMARY KEY,
                    tag TEXT,
                    tagColor TEXT,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
        }
    }

    @After
    fun teardown() {
        conn.close()
    }

    private fun runMigration() {
        conn.createStatement().use { st ->
            TagCaseMigrationSql.statements().forEach(st::executeUpdate)
        }
    }

    private fun insertTask(id: Long, tag: String?, tagColor: String? = null, createdAt: Long = 0L) {
        conn.prepareStatement("INSERT INTO tasks(id, tag, tagColor, createdAt) VALUES(?, ?, ?, ?)").use { ps ->
            ps.setLong(1, id)
            if (tag == null) ps.setNull(2, java.sql.Types.VARCHAR) else ps.setString(2, tag)
            if (tagColor == null) ps.setNull(3, java.sql.Types.VARCHAR) else ps.setString(3, tagColor)
            ps.setLong(4, createdAt)
            ps.executeUpdate()
        }
    }

    private data class Row(val id: Long, val tag: String?, val tagColor: String?)

    private fun rows(): Map<Long, Row> = buildMap {
        conn.createStatement().use { st ->
            st.executeQuery("SELECT id, tag, tagColor FROM tasks ORDER BY id").use { rs ->
                while (rs.next()) {
                    val id = rs.getLong(1)
                    val tag = rs.getString(2)
                    val color = rs.getString(3)
                    put(id, Row(id, tag, color))
                }
            }
        }
    }

    // === Per-task canonicalization ===

    @Test
    fun `lowercase tag is uppercased`() {
        insertTask(1, "work")
        runMigration()
        assertEquals("WORK", rows().getValue(1L).tag)
    }

    @Test
    fun `mixed case tag is uppercased`() {
        insertTask(1, "Work")
        insertTask(2, "WoRk")
        runMigration()
        val r = rows()
        assertEquals("WORK", r.getValue(1L).tag)
        assertEquals("WORK", r.getValue(2L).tag)
    }

    @Test
    fun `whitespace padded tag is trimmed and uppercased`() {
        insertTask(1, "  work  ")
        runMigration()
        assertEquals("WORK", rows().getValue(1L).tag)
    }

    @Test
    fun `blank tag becomes null and color is cleared`() {
        insertTask(1, "   ", tagColor = "red")
        runMigration()
        val r = rows().getValue(1L)
        assertNull(r.tag)
        assertNull(r.tagColor)
    }

    @Test
    fun `null tag is left untouched`() {
        insertTask(1, null, tagColor = null)
        runMigration()
        val r = rows().getValue(1L)
        assertNull(r.tag)
        assertNull(r.tagColor)
    }

    @Test
    fun `already uppercase tag is idempotent`() {
        insertTask(1, "WORK", tagColor = "blue")
        runMigration()
        val r = rows().getValue(1L)
        assertEquals("WORK", r.tag)
        assertEquals("blue", r.tagColor)
    }

    // === Color merge ===

    @Test
    fun `color merge picks most frequent color across case variants`() {
        // "work"+"blue" (x2), "Work"+"red" (x1) → blue wins
        insertTask(1, "work", tagColor = "blue", createdAt = 100)
        insertTask(2, "work", tagColor = "blue", createdAt = 101)
        insertTask(3, "Work", tagColor = "red", createdAt = 200)
        runMigration()
        val r = rows()
        assertEquals("WORK", r.getValue(1L).tag)
        assertEquals("blue", r.getValue(1L).tagColor)
        assertEquals("WORK", r.getValue(3L).tag)
        assertEquals("blue", r.getValue(3L).tagColor)
    }

    @Test
    fun `color merge tiebreaker uses latest createdAt`() {
        // Both "blue" and "red" have count=1 → latest createdAt wins.
        insertTask(1, "work", tagColor = "blue", createdAt = 100)
        insertTask(2, "Work", tagColor = "red", createdAt = 200)
        runMigration()
        val r = rows()
        assertEquals("red", r.getValue(1L).tagColor)
        assertEquals("red", r.getValue(2L).tagColor)
    }

    @Test
    fun `null color is ignored when another variant has a color`() {
        insertTask(1, "work", tagColor = null, createdAt = 500)
        insertTask(2, "Work", tagColor = "green", createdAt = 100)
        runMigration()
        val r = rows()
        assertEquals("green", r.getValue(1L).tagColor)
        assertEquals("green", r.getValue(2L).tagColor)
    }

    @Test
    fun `all-null colors stay null after merge`() {
        insertTask(1, "work", tagColor = null)
        insertTask(2, "Work", tagColor = null)
        runMigration()
        val r = rows()
        assertEquals("WORK", r.getValue(1L).tag)
        assertNull(r.getValue(1L).tagColor)
        assertNull(r.getValue(2L).tagColor)
    }

    @Test
    fun `different canonical tags are isolated`() {
        insertTask(1, "work", tagColor = "blue")
        insertTask(2, "personal", tagColor = "red")
        runMigration()
        val r = rows()
        assertEquals("WORK", r.getValue(1L).tag)
        assertEquals("blue", r.getValue(1L).tagColor)
        assertEquals("PERSONAL", r.getValue(2L).tag)
        assertEquals("red", r.getValue(2L).tagColor)
    }

    // === Idempotency ===

    @Test
    fun `running migration twice produces the same result`() {
        insertTask(1, "work", tagColor = "blue")
        insertTask(2, "Work", tagColor = "red", createdAt = 200)
        insertTask(3, "  ", tagColor = "green")
        insertTask(4, null, tagColor = null)

        runMigration()
        val first = rows()

        runMigration()
        val second = rows()

        assertEquals(first, second)
    }
}
