package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import dev.tuandoan.tasktracker.data.preferences.PrivacyRepository
import dev.tuandoan.tasktracker.diagnostics.di.DiagnosticsScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single façade over the three Firebase SDKs' collection toggles (v1.12.0, FB-05).
 *
 * Why this exists:
 *  - The architecture needs all three SDKs (`FirebaseCrashlytics`, `FirebaseAnalytics`,
 *    `FirebasePerformance`) to stay in lock-step. Fanning out the setEnabled call from
 *    three separate call sites (Settings toggle, startup gate, tests) would make it
 *    easy to forget one. One façade ensures the state is always consistent.
 *  - The Settings toggle handler calls [setEnabled], which persists the flag AND fans
 *    out to all three SDKs in one call. Any future call site that forgets to hit all
 *    three would simply not have been written; the contract lives here.
 *  - The startup gate in [TaskTrackerApplication.onCreate] (FB-06) calls
 *    [initCollectionState] with the value read via
 *    [PrivacyRepository.Companion.readOptInOnce], which fans out to the SDKs
 *    WITHOUT writing to the DataStore. This is correct: startup reads the existing
 *    persisted state rather than overwriting it.
 *
 * Manifest defaults from FB-02 (`firebase_*_collection_enabled=false`) mean the SDKs
 * start disabled on cold install. [initCollectionState] for an opted-out user is a
 * no-op on the first-ever launch (manifest default + explicit false call = same
 * result), but becomes meaningful once the user has opted in and restarted the app.
 *
 * On disable ([setEnabled]`(false)`), we also call
 * [FirebaseCrashlytics.deleteUnsentReports] so any crash report pending upload from
 * before the opt-out is discarded. This is a best-effort guarantee per ADR-003; the
 * SDK processes the request asynchronously and cannot be awaited reliably.
 */
@Singleton
class PrivacyManager @Inject constructor(
    private val privacyRepository: PrivacyRepository,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
    private val performance: FirebasePerformance,
    private val keysWriter: CrashlyticsKeysWriter,
    @DiagnosticsScope private val diagnosticsScope: CoroutineScope,
) {

    /**
     * Sets the three SDK collection flags WITHOUT persisting. Called from
     * [TaskTrackerApplication.onCreate] (FB-06) with the value read from
     * [PrivacyRepository.Companion.readOptInOnce]. Must run BEFORE any other
     * Firebase-touching code on cold start — that's the safety-critical invariant
     * from ADR-003.
     *
     * Separate from [setEnabled] because startup MUST NOT overwrite the persisted
     * flag; it reads and reflects it.
     *
     * On opt-in, the Crashlytics custom keys (FB-10) are written asynchronously on
     * the diagnostics coroutine scope — Crashlytics is tolerant of keys arriving
     * shortly after the first frame, and blocking `onCreate` on DataStore + DB
     * reads would eat into ADR-003's cold-start budget.
     */
    fun initCollectionState(optIn: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(optIn)
        analytics.setAnalyticsCollectionEnabled(optIn)
        performance.isPerformanceCollectionEnabled = optIn
        if (optIn) {
            diagnosticsScope.launch { keysWriter.writeAll() }
        }
    }

    /**
     * Persists the opt-in value AND fans out to the three SDKs. Called from the
     * Settings toggle handler (FB-07) when the user flips the Diagnostics switch.
     *
     * On enable, writes the Crashlytics custom keys (FB-10) inline — we're already
     * in a suspend scope so the main thread isn't blocked, and keys written here
     * attach to any crash later in the session.
     *
     * On disable, also calls [FirebaseCrashlytics.deleteUnsentReports] to discard any
     * crash reports queued for upload — best-effort, async, not awaitable per the
     * SDK contract. Documented in the Privacy Policy (FB-18) so users know a brief
     * window between their toggle-off and the SDK's upload-queue flush exists.
     */
    suspend fun setEnabled(optIn: Boolean) {
        privacyRepository.setDiagnosticsOptIn(optIn)
        crashlytics.setCrashlyticsCollectionEnabled(optIn)
        analytics.setAnalyticsCollectionEnabled(optIn)
        performance.isPerformanceCollectionEnabled = optIn
        if (optIn) {
            keysWriter.writeAll()
        } else {
            crashlytics.deleteUnsentReports()
        }
    }
}
