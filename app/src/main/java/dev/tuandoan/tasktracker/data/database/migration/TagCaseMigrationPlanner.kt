package dev.tuandoan.tasktracker.data.database.migration

import dev.tuandoan.tasktracker.domain.service.TagNormalizer

/**
 * Pure-Kotlin planner for migration v10 → v11 (tag case canonicalization).
 *
 * Why Kotlin instead of SQL: Android's bundled SQLite does not include ICU, so
 * `UPPER('việc')` returns `'VIệC'` (only ASCII letters touched). Using
 * [TagNormalizer] — which calls `String.uppercase(Locale.ROOT)` in the JVM —
 * gives correct, deterministic case folding for every Unicode script.
 *
 * Rules applied by [plan]:
 * - Tags that trim to empty become `NULL`; their `tagColor` is cleared too.
 * - Tags are canonicalized via [TagNormalizer] (trim + `Locale.ROOT` UPPERCASE).
 * - For each canonical tag, the winning color is the most-frequent non-null
 *   color across its rows; ties are broken by the latest `createdAt` among
 *   rows carrying that color.
 * - Only returns [Update]s for rows whose `tag` or `tagColor` actually changes,
 *   so the migration issues the minimum number of writes.
 */
object TagCaseMigrationPlanner {

    data class Row(val id: Long, val tag: String?, val tagColor: String?, val createdAt: Long)

    data class Update(val id: Long, val newTag: String?, val newColor: String?)

    fun plan(rows: List<Row>): List<Update> {
        val canonicalByRowId: Map<Long, String?> = rows.associate { it.id to TagNormalizer.normalize(it.tag) }

        val winningColorByCanonical: Map<String, String?> = rows
            .asSequence()
            .mapNotNull { row ->
                val canonical = canonicalByRowId[row.id] ?: return@mapNotNull null
                canonical to row
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, rowsForTag) -> pickWinningColor(rowsForTag) }

        return rows.mapNotNull { row ->
            val canonical = canonicalByRowId[row.id]
            val newColor = if (canonical == null) null else winningColorByCanonical[canonical]
            if (row.tag == canonical && row.tagColor == newColor) {
                null
            } else {
                Update(id = row.id, newTag = canonical, newColor = newColor)
            }
        }
    }

    private fun pickWinningColor(rowsForTag: List<Row>): String? {
        val colored = rowsForTag.filter { it.tagColor != null }
        if (colored.isEmpty()) return null

        // Group by color → (count, latestCreatedAt). Pick by count DESC, then createdAt DESC.
        return colored
            .groupBy { it.tagColor!! }
            .map { (color, group) -> color to Pair(group.size, group.maxOf { it.createdAt }) }
            .maxWithOrNull(
                compareBy({ it.second.first }, { it.second.second }),
            )
            ?.first
    }
}
