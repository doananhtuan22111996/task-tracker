package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed façade around [FirebasePerformance.newTrace] for the v1.12.0 custom-trace
 * taxonomy (FR-18). Mirrors the closed-set discipline used by [BreadcrumbLogger] (FB-11)
 * and [AnalyticsLogger] (FB-13): callers can't pass a raw trace name — they must pick a
 * variant of [PerformanceTraceName].
 *
 * **Why a typed sealed class instead of `newTrace(String)` passthrough:**
 *  - Trace names are searchable in the Firebase Performance console and become part of
 *    the public schema. A freeform `start("foo_${userId}")` would explode the trace
 *    catalog and could leak identifiers. The sealed type closes that path.
 *  - Adding a trace requires a ticket + ADR update — same governance pattern as
 *    [BreadcrumbCategory] and [AnalyticsEvent].
 *
 * **Why no explicit opt-out check:**
 *  - [FirebasePerformance] respects its own `isPerformanceCollectionEnabled` flag,
 *    which [PrivacyManager] toggles in lock-step with the other two SDKs (FB-05).
 *    Started traces are silently dropped while collection is disabled. Gating here
 *    would duplicate the SDK contract and require a DataStore read on every
 *    composition — the same rule [BreadcrumbLogger] documents.
 *
 * **Returned [PerformanceTrace] is a thin wrapper** so callers don't depend on the
 * Firebase SDK type directly. JVM tests can construct fakes without pulling in
 * Robolectric.
 */
@Singleton
class PerformanceLogger @Inject constructor(private val performance: FirebasePerformance) {

    /**
     * Create and start a custom trace. The caller is responsible for calling
     * [PerformanceTrace.stop] exactly once on the returned handle — pair `start` with
     * `stop` in the same composition / coroutine scope so a recomposition or
     * cancellation can't strand a running trace.
     *
     * Implementation note: [FirebasePerformance.newTrace] returns a [Trace] that has
     * not been started yet, so we explicitly call `start()` before returning. That
     * matches the call-site contract `LaunchedEffect { trace = perf.start(...) }`.
     */
    fun start(name: PerformanceTraceName): PerformanceTrace {
        val trace = performance.newTrace(name.traceName)
        trace.start()
        return FirebasePerformanceTrace(trace)
    }
}

/**
 * Closed set of custom trace names for v1.12.0. Adding a name requires a ticket so
 * the Firebase Performance console schema stays predictable.
 */
sealed class PerformanceTraceName(val traceName: String) {

    /** FR-18 / FB-16: time from `visibleMonth` change to first layout of the new month grid. */
    data object CalendarMonthRender : PerformanceTraceName("calendar_month_render")

    /** FR-18 / FB-17: time spent inside `ImportBackupUseCase.execute`. */
    data object BackupImport : PerformanceTraceName("backup_import")
}

/**
 * Test-friendly handle returned by [PerformanceLogger.start]. Production wraps a real
 * Firebase [Trace]; tests can substitute a fake without depending on Firebase's runtime
 * (no Robolectric on this project).
 */
interface PerformanceTrace {

    /**
     * Add a non-PII attribute (e.g. `record_count_bucket`, `is_cold`) to the running
     * trace. Per the Firebase SDK contract, attribute keys are limited to 40 chars
     * and values to 100 chars; the SDK silently drops oversize entries.
     */
    fun putAttribute(key: String, value: String)

    /** Stop the trace. Must be called exactly once. */
    fun stop()
}

private class FirebasePerformanceTrace(private val trace: Trace) : PerformanceTrace {
    override fun putAttribute(key: String, value: String) {
        trace.putAttribute(key, value)
    }

    override fun stop() {
        trace.stop()
    }
}
