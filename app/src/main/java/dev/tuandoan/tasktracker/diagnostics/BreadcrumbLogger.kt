package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin façade around [FirebaseCrashlytics.log] for recording user-action
 * breadcrumbs that attach to any subsequent crash report (v1.12.0, FB-11).
 *
 * **Why a typed category instead of freeform strings:**
 *  - Breadcrumbs are attached to crash reports as plain text. A freeform `log(String)`
 *    API invites call sites to interpolate user data (`"Opened task: $title"`),
 *    which would leak PII into Crashlytics payloads in violation of ADR-003.
 *  - [BreadcrumbCategory] is a closed set of UI/action verbs. `detail` is intended
 *    for non-identifying context (screen name, filter kind, count bucket). Callers
 *    in FB-12 are reviewed against this contract.
 *
 * **Why no explicit opt-out check:**
 *  - [FirebaseCrashlytics.log] is documented as a no-op while collection is disabled
 *    ([PrivacyManager] toggles that via `setCrashlyticsCollectionEnabled`). Gating
 *    here would duplicate the SDK contract and require a DataStore read on every
 *    UI event, which the cold-start path cannot afford.
 *  - If the user is opted out, `crashlytics.log` drops the string; no payload is
 *    queued or sent.
 *
 * No production callers yet — call sites land in FB-12.
 */
@Singleton
class BreadcrumbLogger @Inject constructor(private val crashlytics: FirebaseCrashlytics) {

    /**
     * Record a breadcrumb. Rendered as `"[CATEGORY] detail"` so filtering in the
     * Crashlytics console by category prefix is straightforward.
     *
     * `detail` must not contain PII (task titles, note content, email addresses,
     * etc.). Safe examples: `"task_list"`, `"filter=active"`, `"bucket=10-49"`.
     */
    fun log(category: BreadcrumbCategory, detail: String) {
        crashlytics.log("[${category.name}] $detail")
    }
}

/**
 * Closed set of breadcrumb categories. Every call site in FB-12 must pick one of
 * these — adding a new variant requires a ticket + ADR update to keep the taxonomy
 * stable. See FB-11 notes in the v1.12.0 backlog.
 */
enum class BreadcrumbCategory {
    /** Navigation between top-level destinations (task list, calendar, archived, stats, settings). */
    NAV,

    /** Task CRUD actions (create, edit, complete, delete, archive, restore). */
    TASK_ACTION,

    /** Filter / sort / search state changes on the task list. */
    FILTER,

    /** Backup import/export flow milestones (start, validate, commit, failure). */
    BACKUP,

    /** Settings-screen interactions (toggle flips, locale change, theme change). */
    SETTINGS,

    /** Reminder / WorkManager lifecycle events (scheduled, fired, cancelled). */
    REMINDER,
}
