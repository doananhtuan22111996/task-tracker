package dev.tuandoan.tasktracker.domain.model

/**
 * Structured tokens extracted from raw task input via shorthand parsing (CAP-10).
 *
 * @property rawText The original input string.
 * @property cleanTitle The task title with extracted tokens stripped and whitespace cleaned.
 * @property dueAt Epoch milliseconds of the inferred due date/time, or null if not detected.
 * @property dueAtHasTime True if an explicit time was parsed; false if only a date was detected (defaults to end-of-day).
 * @property priority Inferred priority level (0 = LOW, 1 = MEDIUM, 2 = HIGH), or null if not detected.
 * @property tag Inferred tag canonicalized via TagNormalizer, or null if not detected.
 */
data class ParsedTaskTokens(
    val rawText: String,
    val cleanTitle: String,
    val dueAt: Long? = null,
    val dueAtHasTime: Boolean = false,
    val priority: Int? = null,
    val tag: String? = null,
) {
    val hasTokens: Boolean
        get() = dueAt != null || priority != null || tag != null
}
