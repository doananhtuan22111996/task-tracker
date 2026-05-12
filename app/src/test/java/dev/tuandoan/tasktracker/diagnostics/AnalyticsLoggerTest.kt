package dev.tuandoan.tasktracker.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test for every [AnalyticsEvent] variant (FB-13).
 *
 * Assertions run against the typed [AnalyticsEvent.params] `Map` directly — the
 * `android.os.Bundle` conversion inside [AnalyticsLogger.log] is Android-runtime
 * territory (no Robolectric on this project) and will be exercised by the Firebase
 * Analytics debug view during FB-14 call-site instrumentation.
 *
 * Critical contracts:
 *   - every event names itself per FR-15
 *   - no event carries a raw task title, tag name, description, or date
 *   - count params are bucketed (FR-16 + shared with FB-10/FB-12)
 *   - enum-backed params serialize to their `paramValue`, not the enum constant name
 */
class AnalyticsLoggerTest {

    // ── Task actions ──────────────────────────────────────────────────────────

    @Test
    fun `TaskCreated exposes name and shape-only params`() {
        val event = AnalyticsEvent.TaskCreated(
            hasDueDate = true,
            hasTag = false,
            priority = AnalyticsPriority.HIGH,
            isRecurring = true,
        )
        assertEquals("task_created", event.eventName)
        assertEquals(true, event.params[AnalyticsEvent.PARAM_HAS_DUE_DATE])
        assertEquals(false, event.params[AnalyticsEvent.PARAM_HAS_TAG])
        // priority serializes as a stable string, not the raw int — enum closes
        // the range-violation leak path a raw Int would open.
        assertEquals("high", event.params[AnalyticsEvent.PARAM_PRIORITY])
        assertEquals(true, event.params[AnalyticsEvent.PARAM_IS_RECURRING])
        // Negative invariant: no content-carrying keys ever.
        assertFalse(event.params.containsKey("title"))
        assertFalse(event.params.containsKey("tag"))
        assertFalse(event.params.containsKey("description"))
        assertFalse(event.params.containsKey("due_at"))
    }

    @Test
    fun `AnalyticsPriority fromTaskPriority maps 0 1 2 correctly and clamps unknowns`() {
        assertEquals(AnalyticsPriority.LOW, AnalyticsPriority.fromTaskPriority(0))
        assertEquals(AnalyticsPriority.MEDIUM, AnalyticsPriority.fromTaskPriority(1))
        assertEquals(AnalyticsPriority.HIGH, AnalyticsPriority.fromTaskPriority(2))
        // Clamp path: a corrupt Int falls back to MEDIUM rather than throwing or
        // shipping garbage to Firebase.
        assertEquals(AnalyticsPriority.MEDIUM, AnalyticsPriority.fromTaskPriority(-1))
        assertEquals(AnalyticsPriority.MEDIUM, AnalyticsPriority.fromTaskPriority(99))
    }

    @Test
    fun `TaskCompleted has the correct name and empty params`() {
        assertEquals("task_completed", AnalyticsEvent.TaskCompleted.eventName)
        assertTrue(AnalyticsEvent.TaskCompleted.params.isEmpty())
    }

    @Test
    fun `TaskArchived has the correct name and empty params`() {
        assertEquals("task_archived", AnalyticsEvent.TaskArchived.eventName)
        assertTrue(AnalyticsEvent.TaskArchived.params.isEmpty())
    }

    @Test
    fun `TaskRestored has the correct name and empty params`() {
        assertEquals("task_restored", AnalyticsEvent.TaskRestored.eventName)
        assertTrue(AnalyticsEvent.TaskRestored.params.isEmpty())
    }

    // ── Calendar funnel ───────────────────────────────────────────────────────

    @Test
    fun `CalendarTabOpened has the correct name and empty params`() {
        assertEquals("calendar_tab_opened", AnalyticsEvent.CalendarTabOpened.eventName)
        assertTrue(AnalyticsEvent.CalendarTabOpened.params.isEmpty())
    }

    @Test
    fun `CalendarDayTapped exposes only has_dated_tasks_that_day and never a date`() {
        val event = AnalyticsEvent.CalendarDayTapped(hasDatedTasksThatDay = true)
        assertEquals("calendar_day_tapped", event.eventName)
        assertEquals(true, event.params[AnalyticsEvent.PARAM_HAS_DATED_TASKS_THAT_DAY])
        // Negative invariant: the raw tap date leaks usage timing; must NEVER be in params.
        assertFalse(event.params.containsKey("date"))
    }

    @Test
    fun `CalendarRecurrenceMaterialized has the correct name and empty params`() {
        assertEquals("calendar_recurrence_materialized", AnalyticsEvent.CalendarRecurrenceMaterialized.eventName)
        assertTrue(AnalyticsEvent.CalendarRecurrenceMaterialized.params.isEmpty())
    }

    // ── Depth ─────────────────────────────────────────────────────────────────

    @Test
    fun `SubtaskAdded has the correct name and empty params`() {
        assertEquals("subtask_added", AnalyticsEvent.SubtaskAdded.eventName)
        assertTrue(AnalyticsEvent.SubtaskAdded.params.isEmpty())
    }

    @Test
    fun `BulkOperationApplied serializes selection_size as bucket string not raw int`() {
        val event = AnalyticsEvent.BulkOperationApplied(
            opType = BulkOpType.ARCHIVE,
            selectionSize = 42,
        )
        assertEquals("bulk_operation_applied", event.eventName)
        assertEquals("archive", event.params[AnalyticsEvent.PARAM_OP_TYPE])
        // Negative invariant: raw 42 never ships; bucket string does.
        assertEquals("10-49", event.params[AnalyticsEvent.PARAM_SELECTION_SIZE])
        assertFalse(event.params[AnalyticsEvent.PARAM_SELECTION_SIZE] is Int)
    }

    @Test
    fun `BulkOperationApplied covers every BulkOpType exhaustively`() {
        BulkOpType.values().forEach { type ->
            val event = AnalyticsEvent.BulkOperationApplied(type, selectionSize = 1)
            assertEquals(type.paramValue, event.params[AnalyticsEvent.PARAM_OP_TYPE])
        }
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    @Test
    fun `BackupExported serializes format as enum paramValue and task_count as bucket`() {
        val event = AnalyticsEvent.BackupExported(
            format = BackupEventFormat.JSON,
            taskCount = 150,
        )
        assertEquals("backup_exported", event.eventName)
        assertEquals("json", event.params[AnalyticsEvent.PARAM_FORMAT])
        assertEquals("50-199", event.params[AnalyticsEvent.PARAM_TASK_COUNT])
    }

    @Test
    fun `BackupImported covers every BackupOutcome`() {
        BackupOutcome.values().forEach { outcome ->
            val event = AnalyticsEvent.BackupImported(
                format = BackupEventFormat.JSON,
                recordCount = 0,
                outcome = outcome,
            )
            assertEquals(outcome.paramValue, event.params[AnalyticsEvent.PARAM_OUTCOME])
        }
    }

    @Test
    fun `BackupImported serializes all three params and buckets record_count`() {
        val event = AnalyticsEvent.BackupImported(
            format = BackupEventFormat.CSV,
            recordCount = 7,
            outcome = BackupOutcome.PARTIAL,
        )
        assertEquals("backup_imported", event.eventName)
        assertEquals("csv", event.params[AnalyticsEvent.PARAM_FORMAT])
        assertEquals("1-9", event.params[AnalyticsEvent.PARAM_RECORD_COUNT])
        assertEquals("partial", event.params[AnalyticsEvent.PARAM_OUTCOME])
    }

    // ── Help ──────────────────────────────────────────────────────────────────

    @Test
    fun `HelpFaqExpanded forwards the section paramValue not the enum name`() {
        val event = AnalyticsEvent.HelpFaqExpanded(section = HelpFaqSection.BACKUP)
        assertEquals("help_faq_expanded", event.eventName)
        // Serializes as the stable lowercase paramValue, not "BACKUP" (the enum constant name).
        assertEquals("backup", event.params[AnalyticsEvent.PARAM_SECTION])
    }

    @Test
    fun `HelpFaqExpanded covers every HelpFaqSection exhaustively`() {
        // Adding a section to the enum without a paramValue mapping fails loudly.
        HelpFaqSection.values().forEach { section ->
            val event = AnalyticsEvent.HelpFaqExpanded(section)
            assertEquals(section.paramValue, event.params[AnalyticsEvent.PARAM_SECTION])
        }
    }
}
