package dev.tuandoan.tasktracker.perf

import dev.tuandoan.tasktracker.data.backup.JsonBackupSerializer
import dev.tuandoan.tasktracker.data.backup.dto.TaskBackupDto
import dev.tuandoan.tasktracker.domain.backup.model.BackupMetadata
import dev.tuandoan.tasktracker.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * CAL-32 perf-trace fixture generator. Emits a 1,000-task `TaskBackupDto` payload as JSON
 * to `app/build/perf/calendar-1000-tasks.json`, ready to import through the Settings →
 * Restore path on a debug device.
 *
 * Not a behavioral test — the "assertions" are invariant checks on the generated payload
 * so we catch a bad fixture before it's imported. The real test (month-grid render ≤ 150ms
 * at 1k tasks, swipe at 60fps) runs manually in Perfetto on a Pixel 6a per the CAL-32
 * test plan.
 *
 * Why inside src/test:
 *  - Piggy-backs on the existing `JsonBackupSerializer` so the fixture is guaranteed
 *    schema-valid (v3 envelope) without shipping any production code.
 *  - `./gradlew testDebugUnitTest --tests "*PerfFixtureGenerator"` regenerates on demand.
 *  - No APK size impact, no dev-menu surface to maintain, no runtime dependency.
 *
 * Fixture shape (1,000 tasks, deterministic via `Random(seed)`):
 *  - 900 concrete non-recurring tasks, `dueAt` spread uniformly across 2026-01-01..2026-12-31
 *  - 80 recurring parents (20 DAILY, 40 WEEKLY w/ Mon+Wed+Fri bitmask, 20 MONTHLY),
 *    `dueAt` early-year so projections fan out across the whole window
 *  - 20 archived tasks so `observeDatedTaskCount` + the CAL-16 card still behave
 *  - Priority mix: 40% LOW(0), 45% MEDIUM(1), 15% HIGH(2) — realistic distribution that
 *    exercises the dot-overflow path on some days
 *  - Tag assignment on ~30% of tasks from a 12-tag pool so tag chips aren't empty
 *  - Subtasks: none. CAL-32 targets the grid, not the editor.
 *
 * Seeded `Random(42)` so the fixture is reproducible across runs — the same JSON bytes
 * produce the same test conditions, so two traces can be compared apples-to-apples.
 */
class CalendarPerfFixtureGenerator {

    @Test
    fun `generate 1000-task fixture for CAL-32 perf trace`() {
        val tasks = buildFixture()

        // Sanity: the ticket is called "1,000-task perf trace" — make sure we emit exactly
        // that count, because downstream NFR thresholds (month render ≤ 150ms, swipe 60fps)
        // are measured at this scale.
        assertEquals(TARGET_TOTAL, tasks.size)

        // Half-decent coverage of the three shapes that stress the grid:
        // concrete-dated, recurring parents, archived-but-present-in-DB.
        assertEquals(CONCRETE_COUNT, tasks.count { it.recurrenceType == RecurrenceType.NONE.value && !it.isArchived })
        assertEquals(RECURRING_COUNT, tasks.count { it.recurrenceType != RecurrenceType.NONE.value })
        assertEquals(ARCHIVED_COUNT, tasks.count { it.isArchived })

        // Every recurring parent must be a chain root (parentRecurringTaskId == null) so
        // `CalendarUseCase.buildDecorations` treats them as projection sources per CAL-24.
        assertTrue(
            "recurring tasks must all be chain roots",
            tasks.filter { it.recurrenceType != RecurrenceType.NONE.value }.all { it.parentRecurringTaskId == null },
        )

        val json = JsonBackupSerializer().serialize(
            tasks = tasks,
            schemaVersion = BackupMetadata.CURRENT_SCHEMA_VERSION,
            exportedAt = EXPORTED_AT_MILLIS,
            appVersion = FIXTURE_APP_VERSION,
        )

        // Dump alongside the build outputs so `./gradlew clean` cleans it up.
        val outDir = File("build/perf").apply { mkdirs() }
        val outFile = File(outDir, "calendar-1000-tasks.json")
        outFile.writeText(json)

        // Echo path + size so the CI log / `/test` output makes the import step obvious.
        println(
            "CAL-32 fixture written: ${outFile.absolutePath} (${"%,d".format(outFile.length())} bytes, " +
                "$TARGET_TOTAL tasks)",
        )
    }

    private fun buildFixture(): List<TaskBackupDto> {
        val rng = Random(FIXTURE_SEED)
        val tasks = buildList<TaskBackupDto>(capacity = TARGET_TOTAL) {
            addAll(buildConcreteTasks(rng, startId = 1L))
            addAll(buildRecurringTasks(rng, startId = CONCRETE_COUNT + 1L))
            addAll(buildArchivedTasks(rng, startId = CONCRETE_COUNT + RECURRING_COUNT + 1L))
        }
        return tasks
    }

    private fun buildConcreteTasks(rng: Random, startId: Long): List<TaskBackupDto> =
        (0 until CONCRETE_COUNT).map { i ->
            val id = startId + i
            // Spread dueAt uniformly across 2026.
            val dayOffset = rng.nextInt(DAYS_IN_2026)
            val dueDate = YEAR_START.plusDays(dayOffset.toLong())
            val dueMillis = dueDate.atStartOfDay(ZONE).toInstant().toEpochMilli()
            val priority = weightedPriority(rng)
            val (tag, color) = maybeTag(rng)
            TaskBackupDto(
                id = id,
                title = "Perf task #$id",
                description = if (rng.nextFloat() < 0.25f) "Auto-seeded for CAL-32" else "",
                dueAt = dueMillis,
                dueAtHasTime = false,
                tag = tag,
                tagColor = color,
                priority = priority,
                createdAt = CREATED_AT_BASE_MILLIS - i * 60_000L,
            )
        }

    private fun buildRecurringTasks(rng: Random, startId: Long): List<TaskBackupDto> {
        val parents = mutableListOf<TaskBackupDto>()
        var id = startId

        // 20 daily chains — anchor Jan 1 so projections cover all 12 months.
        repeat(DAILY_COUNT) {
            val priority = weightedPriority(rng)
            val (tag, color) = maybeTag(rng)
            parents += TaskBackupDto(
                id = id++,
                title = "Daily standup #${parents.size + 1}",
                dueAt = YEAR_START.atStartOfDay(ZONE).toInstant().toEpochMilli(),
                priority = priority,
                tag = tag,
                tagColor = color,
                recurrenceType = RecurrenceType.DAILY.value,
                recurrenceInterval = 1,
            )
        }

        // 40 weekly chains — Mon/Wed/Fri bitmask (0b0010101 = 21 with the 1<<0=Mon
        // convention from CAL-05). Anchor Jan 5 (a Monday in 2026).
        val weeklyAnchor = LocalDate.of(2026, 1, 5)
            .atStartOfDay(ZONE)
            .toInstant()
            .toEpochMilli()
        repeat(WEEKLY_COUNT) {
            val priority = weightedPriority(rng)
            val (tag, color) = maybeTag(rng)
            parents += TaskBackupDto(
                id = id++,
                title = "Weekly review #${parents.size + 1}",
                dueAt = weeklyAnchor,
                priority = priority,
                tag = tag,
                tagColor = color,
                recurrenceType = RecurrenceType.WEEKLY.value,
                recurrenceInterval = 1,
                recurrenceDaysOfWeek = MON_WED_FRI_BITMASK,
            )
        }

        // 20 monthly chains — anchor 15th of Jan so every month gets a projection.
        val monthlyAnchor = LocalDate.of(2026, 1, 15)
            .atStartOfDay(ZONE)
            .toInstant()
            .toEpochMilli()
        repeat(MONTHLY_COUNT) {
            val priority = weightedPriority(rng)
            val (tag, color) = maybeTag(rng)
            parents += TaskBackupDto(
                id = id++,
                title = "Monthly report #${parents.size + 1}",
                dueAt = monthlyAnchor,
                priority = priority,
                tag = tag,
                tagColor = color,
                recurrenceType = RecurrenceType.MONTHLY.value,
                recurrenceInterval = 1,
            )
        }

        return parents
    }

    private fun buildArchivedTasks(rng: Random, startId: Long): List<TaskBackupDto> =
        (0 until ARCHIVED_COUNT).map { i ->
            val id = startId + i
            val dueDate = YEAR_START.plusDays(rng.nextInt(DAYS_IN_2026).toLong())
            val dueMillis = dueDate.atStartOfDay(ZONE).toInstant().toEpochMilli()
            TaskBackupDto(
                id = id,
                title = "Archived perf task #$id",
                dueAt = dueMillis,
                priority = weightedPriority(rng),
                isArchived = true,
                archivedAt = CREATED_AT_BASE_MILLIS + i * 3_600_000L,
            )
        }

    /** ~40% LOW, ~45% MEDIUM, ~15% HIGH — matches "typical productive user" skew. */
    private fun weightedPriority(rng: Random): Int = when (rng.nextFloat()) {
        in 0f..0.40f -> 0 // LOW
        in 0.40f..0.85f -> 1 // MEDIUM
        else -> 2 // HIGH
    }

    /**
     * Returns a `(tag, tagColor)` pair. About 30% of the time both are populated from the
     * canonical pool; otherwise both are null (matching the invariant that orphan colors
     * are dropped on import).
     */
    private fun maybeTag(rng: Random): Pair<String?, String?> {
        if (rng.nextFloat() >= 0.30f) return null to null
        val idx = rng.nextInt(TAG_POOL.size)
        return TAG_POOL[idx] to TAG_COLORS[idx]
    }

    private companion object {
        const val TARGET_TOTAL = 1_000
        const val CONCRETE_COUNT = 900
        const val DAILY_COUNT = 20
        const val WEEKLY_COUNT = 40
        const val MONTHLY_COUNT = 20
        const val RECURRING_COUNT = DAILY_COUNT + WEEKLY_COUNT + MONTHLY_COUNT
        const val ARCHIVED_COUNT = 20

        val ZONE: ZoneId = ZoneId.of("UTC")
        val YEAR_START: LocalDate = LocalDate.of(2026, 1, 1)
        const val DAYS_IN_2026 = 365

        // 1<<0 (Mon) | 1<<2 (Wed) | 1<<4 (Fri) — matches the bitmask convention used by
        // `RecurrenceCalculator.nextWeeklyDate` (CAL-05 regression fix).
        const val MON_WED_FRI_BITMASK = 0b0010101

        const val FIXTURE_SEED = 42L
        const val EXPORTED_AT_MILLIS = 1_767_225_600_000L // 2026-01-01T00:00Z
        const val CREATED_AT_BASE_MILLIS = 1_764_547_200_000L // 2025-12-01T00:00Z
        const val FIXTURE_APP_VERSION = "1.11.0-perf-fixture"

        // Canonical uppercase tags (TagNormalizer invariant).
        val TAG_POOL = listOf(
            "WORK", "HOME", "SHOPPING", "HEALTH", "FINANCE", "LEARN",
            "SIDE PROJECT", "ERRANDS", "TRAVEL", "FAMILY", "BILLS", "HABITS",
        )

        // 10-color palette values from v1.9.0 tag-color feature. Arbitrary stable assignment.
        val TAG_COLORS = listOf(
            "#EF5350", "#AB47BC", "#7E57C2", "#5C6BC0", "#42A5F5", "#26C6DA",
            "#66BB6A", "#FFCA28", "#FF7043", "#8D6E63", "#78909C", "#EC407A",
        )
    }
}
