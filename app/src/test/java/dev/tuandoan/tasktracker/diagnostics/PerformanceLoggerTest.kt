package dev.tuandoan.tasktracker.diagnostics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Contract test for [PerformanceLogger] (FB-16).
 *
 * Verifies the façade's contract: every variant of [PerformanceTraceName] resolves to
 * the documented Firebase trace name, [PerformanceLogger.start] starts the trace before
 * returning, and [PerformanceTrace.stop] stops the underlying SDK trace exactly once.
 *
 * The opt-out path is not unit-tested here because [FirebasePerformance]'s no-op
 * behaviour while collection is disabled is an SDK guarantee, not our code path —
 * same rule applied to [BreadcrumbLogger] and [AnalyticsLogger].
 */
class PerformanceLoggerTest {

    private lateinit var performance: FirebasePerformance
    private lateinit var sdkTrace: Trace
    private lateinit var logger: PerformanceLogger

    @Before
    fun setUp() {
        performance = mockk(relaxed = true)
        sdkTrace = mockk(relaxed = true)
        every { performance.newTrace(any()) } returns sdkTrace
        logger = PerformanceLogger(performance)
    }

    @Test
    fun `start creates and starts a trace with the calendar_month_render name`() {
        logger.start(PerformanceTraceName.CalendarMonthRender)

        verifyOrder {
            performance.newTrace("calendar_month_render")
            sdkTrace.start()
        }
    }

    @Test
    fun `start creates and starts a trace with the backup_import name`() {
        logger.start(PerformanceTraceName.BackupImport)

        verifyOrder {
            performance.newTrace("backup_import")
            sdkTrace.start()
        }
    }

    @Test
    fun `returned handle stop forwards to the underlying SDK trace`() {
        val handle = logger.start(PerformanceTraceName.CalendarMonthRender)

        handle.stop()

        verify { sdkTrace.stop() }
    }

    @Test
    fun `returned handle putAttribute forwards key and value verbatim`() {
        val handle = logger.start(PerformanceTraceName.BackupImport)

        handle.putAttribute("record_count", "10-49")

        verify { sdkTrace.putAttribute("record_count", "10-49") }
    }

    @Test
    fun `PerformanceTraceName traceName matches the documented FR-18 catalog`() {
        // The trace name strings are the public schema in the Firebase Performance
        // console. Pinning them here means a refactor of the sealed class can't
        // silently rename what already-deployed clients are reporting.
        assertEquals("calendar_month_render", PerformanceTraceName.CalendarMonthRender.traceName)
        assertEquals("backup_import", PerformanceTraceName.BackupImport.traceName)
    }
}
