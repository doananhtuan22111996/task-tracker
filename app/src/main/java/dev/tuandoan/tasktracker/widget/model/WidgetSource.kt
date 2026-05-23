package dev.tuandoan.tasktracker.widget.model

/**
 * Closed set of content sources the widget can be configured to show
 * (PRD FR-14, ADR-004 Decision 3). One per-`appWidgetId` choice persists in
 * `widget.preferences_pb` once V13-07 lands; until then `[Today]` is the
 * only source wired into [TaskTrackerWidget.provideGlance].
 *
 * `Today` keeps the v1.12.0 widget's "all active, top-N by pinned/dueAt"
 * semantics so existing placements don't change behavior on upgrade.
 * V13-13 may tighten it to a true today-window query if user feedback
 * indicates confusion; PRD OQ-05 already locks the "overdue cutoff" to
 * strict `dueAt < now`.
 */
sealed interface WidgetSource {

    object Today : WidgetSource

    object Upcoming7d : WidgetSource

    object Pinned : WidgetSource

    /**
     * Tag name MUST be normalized via `TagNormalizer.normalize` before
     * construction. The widget config writer (V13-08) is responsible for
     * normalization; consumers compare against the stored tag column directly.
     */
    data class Tag(val name: String) : WidgetSource
}
