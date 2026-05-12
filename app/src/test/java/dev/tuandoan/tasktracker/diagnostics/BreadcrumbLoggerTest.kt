package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Contract test for [BreadcrumbLogger] (FB-11).
 *
 * Verifies the render format (`[CATEGORY] detail`) and that every
 * [BreadcrumbCategory] variant reaches Crashlytics unchanged. The opt-out case
 * is not unit-tested here because `FirebaseCrashlytics.log`'s no-op behaviour
 * while collection is disabled is an SDK guarantee, not our code path.
 */
class BreadcrumbLoggerTest {

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var logger: BreadcrumbLogger

    @Before
    fun setUp() {
        crashlytics = mockk(relaxed = true)
        logger = BreadcrumbLogger(crashlytics)
    }

    @Test
    fun `log renders category in brackets followed by detail`() {
        logger.log(BreadcrumbCategory.NAV, "task_list")
        verify { crashlytics.log("[NAV] task_list") }
    }

    @Test
    fun `log preserves detail whitespace and punctuation without sanitising`() {
        // Detail is the call site's responsibility per the KDoc — we do not
        // strip or re-encode. A test pin makes that contract explicit.
        logger.log(BreadcrumbCategory.FILTER, "filter=active;sort=due")
        verify { crashlytics.log("[FILTER] filter=active;sort=due") }
    }

    @Test
    fun `log accepts every BreadcrumbCategory variant`() {
        // Exhaustive over the enum so adding a variant without covering it here
        // fails loudly. Keep the `when` exhaustive (no `else`).
        BreadcrumbCategory.values().forEach { category ->
            logger.log(category, "probe")
            verify { crashlytics.log("[${category.name}] probe") }
        }
    }

    @Test
    fun `log with empty detail still emits category prefix`() {
        logger.log(BreadcrumbCategory.SETTINGS, "")
        verify { crashlytics.log("[SETTINGS] ") }
    }
}
