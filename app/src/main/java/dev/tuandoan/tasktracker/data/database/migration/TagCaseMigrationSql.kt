package dev.tuandoan.tasktracker.data.database.migration

/**
 * SQL statements for migration v10 → v11: canonicalize tag values to UPPERCASE + trimmed.
 *
 * Extracted as constants so the JVM-level migration test can execute the exact same SQL
 * that the app runs, without relying on AndroidTest infrastructure.
 *
 * Semantics:
 * - Blank tags (trim → empty) become NULL and lose their color.
 * - For each canonical tag (UPPER(TRIM(tag))), the winning color is the most-frequent
 *   non-null color among that tag's tasks. Ties are broken by MAX(createdAt).
 * - All tasks sharing a canonical tag end up with the same spelling and winning color.
 */
object TagCaseMigrationSql {

    const val CLEAR_BLANK_TAGS = """
        UPDATE tasks
        SET tag = NULL, tagColor = NULL
        WHERE tag IS NOT NULL AND TRIM(tag) = ''
    """

    const val DROP_TEMP_WINNER = "DROP TABLE IF EXISTS tag_color_winner"

    const val CREATE_TEMP_WINNER = """
        CREATE TEMP TABLE tag_color_winner (
            canonical_tag TEXT PRIMARY KEY,
            winner_color TEXT
        )
    """

    const val POPULATE_WINNER = """
        INSERT INTO tag_color_winner (canonical_tag, winner_color)
        SELECT canonical_tag, (
            SELECT tagColor
            FROM tasks t2
            WHERE UPPER(TRIM(t2.tag)) = outer_q.canonical_tag
              AND t2.tagColor IS NOT NULL
            GROUP BY t2.tagColor
            ORDER BY COUNT(*) DESC, MAX(t2.createdAt) DESC
            LIMIT 1
        ) AS winner_color
        FROM (
            SELECT DISTINCT UPPER(TRIM(tag)) AS canonical_tag
            FROM tasks
            WHERE tag IS NOT NULL AND TRIM(tag) <> ''
        ) AS outer_q
    """

    const val APPLY_CANONICAL = """
        UPDATE tasks
        SET
            tagColor = (
                SELECT winner_color FROM tag_color_winner
                WHERE canonical_tag = UPPER(TRIM(tasks.tag))
            ),
            tag = UPPER(TRIM(tag))
        WHERE tag IS NOT NULL AND TRIM(tag) <> ''
    """

    /**
     * Returns the full ordered sequence of statements to execute for this migration.
     * The app and test both call this to guarantee behavioral parity.
     */
    fun statements(): List<String> = listOf(
        CLEAR_BLANK_TAGS,
        DROP_TEMP_WINNER,
        CREATE_TEMP_WINNER,
        POPULATE_WINNER,
        APPLY_CANONICAL,
        DROP_TEMP_WINNER,
    )
}
