package dev.tuandoan.tasktracker.diagnostics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed façade around [FirebaseAnalytics.logEvent] for the v1.12.0 Analytics event
 * taxonomy (FR-15). Every event is a [AnalyticsEvent] sealed subtype — callers can't
 * pass a raw string, and the params a subtype accepts are typed (booleans, bucketed
 * counts, enum strings) so there's no code path that interpolates user content into
 * an event parameter.
 *
 * **Why a typed sealed class instead of ****`logEvent(name, params)`**** passthrough:**
 *  - Freeform `logEvent("task_completed", bundleOf("title" to task.title))` compiles
 *    fine but violates FR-16 ("no PII in event parameters"). Typed subtypes close
 *    that path: there's no way to pass a task title to [AnalyticsEvent.TaskCompleted]
 *    because it doesn't take one.
 *  - Count params ([AnalyticsEvent.BackupExported.taskCount] etc.) accept `Int` but
 *    serialize as the 5-bucket string shared with FB-10 Crashlytics keys + FB-12
 *    backup breadcrumbs. One source of truth for the bucketing rule is
 *    [bucketTaskCount]; call sites pass raw ints, the façade applies the privacy
 *    filter.
 *
 * **Why no explicit opt-out check:**
 *  - [FirebaseAnalytics.logEvent] is a no-op while collection is disabled
 *    ([PrivacyManager] toggles that via `setAnalyticsCollectionEnabled`). Gating
 *    here would duplicate the SDK contract and require a DataStore read on every
 *    UI event, the same rule `BreadcrumbLogger` (FB-11) documents.
 *
 * **Built-in events ****`app_open`****, ****`screen_view`****, ****`first_open`**** are NOT part of this
 * sealed class** — Firebase's default auto-logging fires them and we don't want
 * a second code path. The 12 events here are the ones we explicitly control.
 *
 * No production callers yet — call sites land in FB-14.
 */
@Singleton
class AnalyticsLogger @Inject constructor(private val analytics: FirebaseAnalytics) {

    /**
     * Record an analytics event. The event's [AnalyticsEvent.eventName] and
     * [AnalyticsEvent.params] are both produced by the sealed subtype so the
     * façade can't accidentally drop or misroute fields.
     *
     * The subtype returns a typed [Map] and this fn converts to Firebase's
     * [Bundle]; that way tests can assert on the map directly without needing
     * Robolectric (the project's existing JVM-only testing convention).
     */
    fun log(event: AnalyticsEvent) {
        analytics.logEvent(event.eventName, event.params.toBundle())
    }

    private fun Map<String, Any>.toBundle(): Bundle {
        if (isEmpty()) return Bundle.EMPTY
        val bundle = Bundle(size)
        for ((key, value) in this) {
            when (value) {
                is Boolean -> bundle.putBoolean(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is String -> bundle.putString(key, value)
                else -> error("Unsupported Analytics param type: ${value::class.java.name}")
            }
        }
        return bundle
    }
}

/**
 * Closed set of Analytics events for v1.12.0. Covers the 12 events we control; the
 * 3 Firebase-built-in events (`app_open`, `screen_view`, `first_open`) fire from
 * Firebase's own auto-logging and are intentionally not represented here.
 *
 * Every param is a boolean, a bucketed count, or a known-vocabulary enum. Adding
 * a new variant requires a ticket + PRD update to keep the taxonomy stable.
 */
sealed class AnalyticsEvent {

    /** The Firebase event name. Must be lowercase_snake_case and ≤ 40 chars per the SDK. */
    abstract val eventName: String

    /**
     * Typed params map. Values are restricted to `Boolean` / `Int` / `Long` / `Double`
     * / `String` by convention — the façade converts to `Bundle` and `error()`s on
     * unsupported types so a future variant can't slip through a leaky generic.
     */
    abstract val params: Map<String, Any>

    // ── Task actions ─────────────────────────────────────────────────────────

    /**
     * FR-15: `task_created`. Params carry shape signals, not content. `priority` is
     * an [AnalyticsPriority] enum (not a raw int) so a call-site bug can't ship an
     * out-of-range value to Firebase where it'd corrupt the analytics console.
     */
    data class TaskCreated(
        val hasDueDate: Boolean,
        val hasTag: Boolean,
        val priority: AnalyticsPriority,
        val isRecurring: Boolean,
    ) : AnalyticsEvent() {
        override val eventName: String = "task_created"
        override val params: Map<String, Any> = mapOf(
            PARAM_HAS_DUE_DATE to hasDueDate,
            PARAM_HAS_TAG to hasTag,
            PARAM_PRIORITY to priority.paramValue,
            PARAM_IS_RECURRING to isRecurring,
        )
    }

    /** FR-15: `task_completed`. Zero params — the event itself is the signal. */
    data object TaskCompleted : AnalyticsEvent() {
        override val eventName: String = "task_completed"
        override val params: Map<String, Any> = emptyMap()
    }

    /** FR-15: `task_archived`. */
    data object TaskArchived : AnalyticsEvent() {
        override val eventName: String = "task_archived"
        override val params: Map<String, Any> = emptyMap()
    }

    /** FR-15: `task_restored`. */
    data object TaskRestored : AnalyticsEvent() {
        override val eventName: String = "task_restored"
        override val params: Map<String, Any> = emptyMap()
    }

    // ── Calendar funnel ──────────────────────────────────────────────────────

    /** FR-15: `calendar_tab_opened`. */
    data object CalendarTabOpened : AnalyticsEvent() {
        override val eventName: String = "calendar_tab_opened"
        override val params: Map<String, Any> = emptyMap()
    }

    /**
     * FR-15: `calendar_day_tapped`.
     * @param hasDatedTasksThatDay whether the tapped day had any concrete or projected tasks.
     */
    data class CalendarDayTapped(val hasDatedTasksThatDay: Boolean) : AnalyticsEvent() {
        override val eventName: String = "calendar_day_tapped"
        override val params: Map<String, Any> = mapOf(
            PARAM_HAS_DATED_TASKS_THAT_DAY to hasDatedTasksThatDay,
        )
    }

    /** FR-15: `calendar_recurrence_materialized`. Marks projection → concrete. */
    data object CalendarRecurrenceMaterialized : AnalyticsEvent() {
        override val eventName: String = "calendar_recurrence_materialized"
        override val params: Map<String, Any> = emptyMap()
    }

    // ── Depth ────────────────────────────────────────────────────────────────

    /** FR-15: `subtask_added`. */
    data object SubtaskAdded : AnalyticsEvent() {
        override val eventName: String = "subtask_added"
        override val params: Map<String, Any> = emptyMap()
    }

    /**
     * FR-15: `bulk_operation_applied`.
     * @param opType one of [BulkOpType] — enum-backed so a caller can't slip a user
     *   string in.
     * @param selectionSize the raw selection count; serialized as the shared
     *   [bucketTaskCount] bucket ("1-9", "10-49", …) rather than the raw int to
     *   protect extreme-value users.
     */
    data class BulkOperationApplied(val opType: BulkOpType, val selectionSize: Int) : AnalyticsEvent() {
        override val eventName: String = "bulk_operation_applied"
        override val params: Map<String, Any> = mapOf(
            PARAM_OP_TYPE to opType.paramValue,
            PARAM_SELECTION_SIZE to bucketTaskCount(selectionSize),
        )
    }

    // ── Backup ───────────────────────────────────────────────────────────────

    /**
     * FR-15: `backup_exported`. [taskCount] is bucketed via [bucketTaskCount]; raw
     * counts at the extremes (1 task, 10_000 tasks) correlate too tightly with a
     * specific user.
     */
    data class BackupExported(val format: BackupEventFormat, val taskCount: Int) : AnalyticsEvent() {
        override val eventName: String = "backup_exported"
        override val params: Map<String, Any> = mapOf(
            PARAM_FORMAT to format.paramValue,
            PARAM_TASK_COUNT to bucketTaskCount(taskCount),
        )
    }

    /**
     * FR-15: `backup_imported`. Same count-bucketing rule as [BackupExported].
     */
    data class BackupImported(val format: BackupEventFormat, val recordCount: Int, val outcome: BackupOutcome) :
        AnalyticsEvent() {
        override val eventName: String = "backup_imported"
        override val params: Map<String, Any> = mapOf(
            PARAM_FORMAT to format.paramValue,
            PARAM_RECORD_COUNT to bucketTaskCount(recordCount),
            PARAM_OUTCOME to outcome.paramValue,
        )
    }

    // ── Help ─────────────────────────────────────────────────────────────────

    /**
     * FR-15: `help_faq_expanded`.
     * @param section one of the 9 FAQ sections from `HelpScreen` (see [HelpFaqSection]).
     *   Enum-backed so a call site can't pass a localized title or a user-typed string.
     */
    data class HelpFaqExpanded(val section: HelpFaqSection) : AnalyticsEvent() {
        override val eventName: String = "help_faq_expanded"
        override val params: Map<String, Any> = mapOf(PARAM_SECTION to section.paramValue)
    }

    // ── Widget lifecycle (FR-19 / V13-14) ────────────────────────────────────

    /**
     * FR-19: `widget_added`. Fired on explicit config-success (user taps Confirm in
     * [WidgetConfigureActivity]). The source param is the one the user chose, so
     * `widget_added` and `widget_configured` are always emitted together on first
     * placement via the configure flow (OQ-04).
     */
    data object WidgetAdded : AnalyticsEvent() {
        override val eventName: String = "widget_added"
        override val params: Map<String, Any> = emptyMap()
    }

    /** FR-19: `widget_removed`. Fired per-id in [onDeleted]. */
    data object WidgetRemoved : AnalyticsEvent() {
        override val eventName: String = "widget_removed"
        override val params: Map<String, Any> = emptyMap()
    }

    /**
     * FR-19: `widget_configured`. Fired when the user confirms a source selection.
     * @param source the chosen [WidgetAnalyticsSource] — enum-backed so a call site
     *   can't pass a tag name or other user-supplied string.
     */
    data class WidgetConfigured(val source: WidgetAnalyticsSource) : AnalyticsEvent() {
        override val eventName: String = "widget_configured"
        override val params: Map<String, Any> = mapOf(PARAM_WIDGET_SOURCE to source.paramValue)
    }

    /** FR-19: `widget_task_completed`. Fired when a task is completed via the widget row. */
    data object WidgetTaskCompleted : AnalyticsEvent() {
        override val eventName: String = "widget_task_completed"
        override val params: Map<String, Any> = emptyMap()
    }

    /**
     * FR-19: `widget_resized`. Fired when the rendered layout size changes between
     * consecutive [provideGlance] calls for the same widget id.
     * @param fromSize the previous [WidgetAnalyticsSize] key.
     * @param toSize the new [WidgetAnalyticsSize] key.
     */
    data class WidgetResized(val fromSize: WidgetAnalyticsSize, val toSize: WidgetAnalyticsSize) : AnalyticsEvent() {
        override val eventName: String = "widget_resized"
        override val params: Map<String, Any> = mapOf(
            PARAM_FROM_SIZE to fromSize.paramValue,
            PARAM_TO_SIZE to toSize.paramValue,
        )
    }

    companion object {
        // Parameter name constants — kept consistent across related events where
        // possible so Firebase funnel builders can reuse them.
        const val PARAM_HAS_DUE_DATE = "has_due_date"
        const val PARAM_HAS_TAG = "has_tag"
        const val PARAM_PRIORITY = "priority"
        const val PARAM_IS_RECURRING = "is_recurring"
        const val PARAM_HAS_DATED_TASKS_THAT_DAY = "has_dated_tasks_that_day"
        const val PARAM_OP_TYPE = "op_type"
        const val PARAM_SELECTION_SIZE = "selection_size"
        const val PARAM_FORMAT = "format"
        const val PARAM_TASK_COUNT = "task_count"
        const val PARAM_RECORD_COUNT = "record_count"
        const val PARAM_OUTCOME = "outcome"
        const val PARAM_SECTION = "section"
        const val PARAM_WIDGET_SOURCE = "source"
        const val PARAM_FROM_SIZE = "from_size"
        const val PARAM_TO_SIZE = "to_size"
    }
}

/**
 * Closed set of bulk-operation kinds. Keeps [AnalyticsEvent.BulkOperationApplied.opType]
 * from accepting an arbitrary string (which a call site might populate from user input).
 */
enum class BulkOpType(val paramValue: String) {
    COMPLETE("complete"),
    ARCHIVE("archive"),
    DELETE("delete"),
    RESTORE("restore"),
    SET_PRIORITY("set_priority"),
    SET_TAG("set_tag"),
}

/** Closed set of backup formats. Mirrors `domain.backup.model.BackupFormat` but lives
 *  on the diagnostics side so Analytics doesn't depend on the backup module's enum. */
enum class BackupEventFormat(val paramValue: String) {
    JSON("json"),
    CSV("csv"),
}

/** Closed set of backup-import outcomes. */
enum class BackupOutcome(val paramValue: String) {
    SUCCESS("success"),
    PARTIAL("partial"),
    ERROR("error"),
}

/**
 * Closed set of task priorities for [AnalyticsEvent.TaskCreated]. Matches `Task.priority`'s
 * stored int values (0/1/2) but keeps Analytics decoupled from the storage schema so a
 * future priority renumber doesn't silently change what Firebase receives.
 */
enum class AnalyticsPriority(val paramValue: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    ;

    companion object {
        /**
         * Maps the raw `Task.priority` int (0=LOW, 1=MEDIUM, 2=HIGH) to the enum.
         * Defaults to [MEDIUM] for any out-of-range value so a call site with a
         * corrupt int doesn't skip the event or ship garbage.
         */
        fun fromTaskPriority(priority: Int): AnalyticsPriority = when (priority) {
            0 -> LOW
            1 -> MEDIUM
            2 -> HIGH
            else -> MEDIUM
        }
    }
}

/**
 * Closed set of FAQ sections rendered on `HelpScreen`. Keeps
 * [AnalyticsEvent.HelpFaqExpanded.section] from accepting a localized title or
 * an arbitrary caller string — the 9 values mirror `help_section_*` string keys
 * in `res/values/strings.xml`.
 */
enum class HelpFaqSection(val paramValue: String) {
    GETTING_STARTED("getting_started"),
    MANAGING_TASKS("managing_tasks"),
    CALENDAR("calendar"),
    SUBTASKS("subtasks"),
    BATCH_OPS("batch_ops"),
    REMINDERS("reminders"),
    ARCHIVE("archive"),
    STATS("stats"),
    BACKUP("backup"),
}

/**
 * Closed set of widget source labels for [AnalyticsEvent.WidgetConfigured].
 * Mirrors [dev.tuandoan.tasktracker.widget.model.WidgetSource] variants but lives on
 * the diagnostics side so Analytics doesn't depend on the widget module's sealed
 * interface, and tag names (user-supplied) are collapsed to a single "tag" bucket.
 */
enum class WidgetAnalyticsSource(val paramValue: String) {
    TODAY("today"),
    UPCOMING_7D("upcoming_7d"),
    PINNED("pinned"),
    TAG("tag"),
}

/**
 * Closed set of widget layout sizes for [AnalyticsEvent.WidgetResized].
 * Maps to the three [dev.tuandoan.tasktracker.widget.ui.WidgetLayoutMode] variants
 * so the Firebase funnel can distinguish resize paths without raw dp values.
 */
enum class WidgetAnalyticsSize(val paramValue: String) {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
}
