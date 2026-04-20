package dev.tuandoan.tasktracker.data.database.migration

import dev.tuandoan.tasktracker.data.database.migration.TagCaseMigrationPlanner.Row
import dev.tuandoan.tasktracker.data.database.migration.TagCaseMigrationPlanner.Update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagCaseMigrationPlannerTest {

    private fun row(id: Long, tag: String?, color: String? = null, createdAt: Long = 0L) =
        Row(id = id, tag = tag, tagColor = color, createdAt = createdAt)

    private fun List<Update>.byId(): Map<Long, Update> = associateBy { it.id }

    // === Per-row canonicalization ===

    @Test
    fun `lowercase tag is uppercased`() {
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "work")))
        assertEquals("WORK", updates.byId().getValue(1L).newTag)
    }

    @Test
    fun `mixed case tag is uppercased`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(row(1, "Work"), row(2, "WoRk")),
        ).byId()
        assertEquals("WORK", updates.getValue(1L).newTag)
        assertEquals("WORK", updates.getValue(2L).newTag)
    }

    @Test
    fun `whitespace padded tag is trimmed and uppercased`() {
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "  work  ")))
        assertEquals("WORK", updates.byId().getValue(1L).newTag)
    }

    @Test
    fun `blank tag becomes null and color is cleared`() {
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "   ", color = "red")))
        val u = updates.byId().getValue(1L)
        assertNull(u.newTag)
        assertNull(u.newColor)
    }

    @Test
    fun `null tag with null color produces no update`() {
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, null, null)))
        assertTrue(updates.isEmpty())
    }

    @Test
    fun `already uppercase tag with matching color produces no update`() {
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "WORK", "blue")))
        assertTrue(updates.isEmpty())
    }

    // === Non-ASCII (regression: SQLite UPPER is ASCII-only) ===

    @Test
    fun `vietnamese tag uses Locale ROOT uppercase`() {
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "việc")))
        assertEquals("VIỆC", updates.byId().getValue(1L).newTag)
    }

    @Test
    fun `vietnamese case variants merge into one canonical form`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "việc", color = "blue", createdAt = 100),
                row(2, "Việc", color = "blue", createdAt = 101),
                row(3, "VIỆC", color = "red", createdAt = 200),
            ),
        ).byId()
        // Rows 1 and 2 must be updated to VIỆC; row 3 only updates color if it differs.
        assertEquals("VIỆC", updates.getValue(1L).newTag)
        assertEquals("VIỆC", updates.getValue(2L).newTag)
        assertEquals("blue", updates.getValue(1L).newColor)
        assertEquals("blue", updates.getValue(2L).newColor)
        // Row 3: tag stays VIỆC, but color flips from red to blue (blue won by count).
        assertEquals("VIỆC", updates.getValue(3L).newTag)
        assertEquals("blue", updates.getValue(3L).newColor)
    }

    @Test
    fun `turkish dotless i uses Locale ROOT`() {
        // Locale.ROOT: "i" → "I". Device-default Turkish would produce "İ" (dotted capital),
        // which would split canonical forms across devices.
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "idea")))
        assertEquals("IDEA", updates.byId().getValue(1L).newTag)
    }

    @Test
    fun `devanagari tag passes through unchanged`() {
        // Devanagari has no case — should round-trip as-is.
        val updates = TagCaseMigrationPlanner.plan(listOf(row(1, "काम")))
        // Already canonical → no update emitted.
        assertTrue(updates.isEmpty())
    }

    // === Color merge ===

    @Test
    fun `color merge picks most frequent color across case variants`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "work", color = "blue", createdAt = 100),
                row(2, "work", color = "blue", createdAt = 101),
                row(3, "Work", color = "red", createdAt = 200),
            ),
        ).byId()
        assertEquals("blue", updates.getValue(1L).newColor)
        assertEquals("blue", updates.getValue(3L).newColor)
    }

    @Test
    fun `color merge tiebreaker uses latest createdAt`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "work", color = "blue", createdAt = 100),
                row(2, "Work", color = "red", createdAt = 200),
            ),
        ).byId()
        assertEquals("red", updates.getValue(1L).newColor)
        assertEquals("red", updates.getValue(2L).newColor)
    }

    @Test
    fun `null color is ignored when another variant has a color`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "work", color = null, createdAt = 500),
                row(2, "Work", color = "green", createdAt = 100),
            ),
        ).byId()
        assertEquals("green", updates.getValue(1L).newColor)
        assertEquals("green", updates.getValue(2L).newColor)
    }

    @Test
    fun `all null colors stay null after merge`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "work", null),
                row(2, "Work", null),
            ),
        ).byId()
        assertEquals("WORK", updates.getValue(1L).newTag)
        assertNull(updates.getValue(1L).newColor)
        assertNull(updates.getValue(2L).newColor)
    }

    @Test
    fun `different canonical tags are isolated`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "work", "blue"),
                row(2, "personal", "red"),
            ),
        ).byId()
        assertEquals("WORK", updates.getValue(1L).newTag)
        assertEquals("blue", updates.getValue(1L).newColor)
        assertEquals("PERSONAL", updates.getValue(2L).newTag)
        assertEquals("red", updates.getValue(2L).newColor)
    }

    // === Write minimization ===

    @Test
    fun `plan returns updates only for rows that need to change`() {
        val updates = TagCaseMigrationPlanner.plan(
            listOf(
                row(1, "WORK", "blue"), // already canonical, color wins — skip
                row(2, "work", "blue"), // needs tag update
                row(3, null, null), // untouched, skip
            ),
        )
        assertEquals(setOf(2L), updates.map { it.id }.toSet())
    }

    // === Idempotency ===

    @Test
    fun `running plan twice produces empty second pass`() {
        val initial = listOf(
            row(1, "work", "blue"),
            row(2, "Work", "red", createdAt = 200),
            row(3, "  ", "green"),
            row(4, null, null),
        )
        val firstPass = TagCaseMigrationPlanner.plan(initial)

        val afterFirst = initial.map { orig ->
            val u = firstPass.firstOrNull { it.id == orig.id }
            if (u == null) orig else orig.copy(tag = u.newTag, tagColor = u.newColor)
        }

        val secondPass = TagCaseMigrationPlanner.plan(afterFirst)
        assertTrue("Idempotent: second pass should emit no updates", secondPass.isEmpty())
    }
}
