package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import dev.tuandoan.tasktracker.data.preferences.PrivacyRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Contract test for [PrivacyManager] (FB-05).
 *
 * Every Firebase SDK is a mock — the point is to verify fan-out semantics and call
 * ordering, not to test Firebase itself. `relaxed = true` keeps setup terse since all
 * setXxxCollectionEnabled returns are void.
 *
 * Load-bearing properties tested here:
 *  1. `initCollectionState` touches all 3 SDKs and does NOT write to the repository
 *     (startup-read path must not overwrite the persisted flag).
 *  2. `setEnabled(true)` persists AND fans out to all 3 SDKs, no deleteUnsentReports.
 *  3. `setEnabled(false)` persists AND fans out to all 3 SDKs AND calls
 *     deleteUnsentReports — the disable-side cleanup that discards pending crash
 *     reports per ADR-003.
 *  4. Persistence-before-SDK ordering in `setEnabled` — if the DataStore write ever
 *     starts throwing, we'd rather have the SDKs in the old state than enabled with
 *     no persisted backing.
 *  5. Disable-before-delete ordering in `setEnabled(false)` — the
 *     Crashlytics.deleteUnsentReports must execute against an already-disabled SDK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyManagerTest {

    private lateinit var repository: PrivacyRepository
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var performance: FirebasePerformance
    private lateinit var manager: PrivacyManager

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        crashlytics = mockk(relaxed = true)
        analytics = mockk(relaxed = true)
        performance = mockk(relaxed = true)
        manager = PrivacyManager(repository, crashlytics, analytics, performance)
    }

    // ── initCollectionState ──────────────────────────────────────────────────

    @Test
    fun `initCollectionState false disables all three SDKs`() {
        manager.initCollectionState(optIn = false)

        verify { crashlytics.setCrashlyticsCollectionEnabled(false) }
        verify { analytics.setAnalyticsCollectionEnabled(false) }
        verify { performance.isPerformanceCollectionEnabled = false }
    }

    @Test
    fun `initCollectionState true enables all three SDKs`() {
        manager.initCollectionState(optIn = true)

        verify { crashlytics.setCrashlyticsCollectionEnabled(true) }
        verify { analytics.setAnalyticsCollectionEnabled(true) }
        verify { performance.isPerformanceCollectionEnabled = true }
    }

    @Test
    fun `initCollectionState does not write to the repository`() {
        // The startup-read path reflects the persisted flag; it must not overwrite it.
        // If startup accidentally persisted a default, a crash mid-startup could flip
        // the user's opt-out back — catastrophic for ADR-003's structural guarantee.
        manager.initCollectionState(optIn = false)

        coVerify(exactly = 0) { repository.setDiagnosticsOptIn(any()) }
    }

    @Test
    fun `initCollectionState does not call deleteUnsentReports`() {
        // deleteUnsentReports is a setEnabled(false) concern (user actively opting out).
        // Startup reflecting a false persisted value must not flush crash payloads.
        manager.initCollectionState(optIn = false)

        verify(exactly = 0) { crashlytics.deleteUnsentReports() }
    }

    // ── setEnabled(true) ─────────────────────────────────────────────────────

    @Test
    fun `setEnabled true persists then enables all three SDKs`() = runTest(UnconfinedTestDispatcher()) {
        manager.setEnabled(optIn = true)

        coVerify { repository.setDiagnosticsOptIn(true) }
        verify { crashlytics.setCrashlyticsCollectionEnabled(true) }
        verify { analytics.setAnalyticsCollectionEnabled(true) }
        verify { performance.isPerformanceCollectionEnabled = true }
    }

    @Test
    fun `setEnabled true does not call deleteUnsentReports`() = runTest(UnconfinedTestDispatcher()) {
        manager.setEnabled(optIn = true)

        verify(exactly = 0) { crashlytics.deleteUnsentReports() }
    }

    @Test
    fun `setEnabled true writes repository before enabling any SDK`() = runTest(UnconfinedTestDispatcher()) {
        // Persistence-before-SDK ordering across ALL three SDKs, not just Crashlytics.
        // Protects against a disk failure leaving any SDK enabled with no persisted
        // backing. A future refactor that accidentally moves `performance.isPerformance...`
        // before the repository write would get caught here.
        manager.setEnabled(optIn = true)

        coVerifyOrder {
            repository.setDiagnosticsOptIn(true)
            crashlytics.setCrashlyticsCollectionEnabled(true)
            analytics.setAnalyticsCollectionEnabled(true)
            performance.isPerformanceCollectionEnabled = true
        }
    }

    // ── setEnabled(false) ────────────────────────────────────────────────────

    @Test
    fun `setEnabled false persists and disables all three SDKs`() = runTest(UnconfinedTestDispatcher()) {
        manager.setEnabled(optIn = false)

        coVerify { repository.setDiagnosticsOptIn(false) }
        verify { crashlytics.setCrashlyticsCollectionEnabled(false) }
        verify { analytics.setAnalyticsCollectionEnabled(false) }
        verify { performance.isPerformanceCollectionEnabled = false }
    }

    @Test
    fun `setEnabled false calls deleteUnsentReports to flush pending crash payloads`() =
        runTest(UnconfinedTestDispatcher()) {
            manager.setEnabled(optIn = false)

            verify { crashlytics.deleteUnsentReports() }
        }

    @Test
    fun `setEnabled false disables Crashlytics before deleteUnsentReports`() = runTest(UnconfinedTestDispatcher()) {
        // Disabling first means the delete request executes against a disabled SDK.
        // Reversing could race with a crash fired between the delete and the
        // disable call.
        manager.setEnabled(optIn = false)

        verifyOrder {
            crashlytics.setCrashlyticsCollectionEnabled(false)
            crashlytics.deleteUnsentReports()
        }
    }

    @Test
    fun `setEnabled false writes repository before disabling any SDK`() = runTest(UnconfinedTestDispatcher()) {
        // Symmetric with the enable path: persistence-before-SDK ordering across all
        // three SDKs on the disable path too. Combined with the disable-before-delete
        // test above, this nails down the full 5-step sequence (persist, disable x3,
        // deleteUnsentReports) as a single invariant.
        manager.setEnabled(optIn = false)

        coVerifyOrder {
            repository.setDiagnosticsOptIn(false)
            crashlytics.setCrashlyticsCollectionEnabled(false)
            analytics.setAnalyticsCollectionEnabled(false)
            performance.isPerformanceCollectionEnabled = false
            crashlytics.deleteUnsentReports()
        }
    }
}
