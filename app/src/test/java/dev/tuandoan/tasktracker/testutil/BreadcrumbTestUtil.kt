package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.diagnostics.AnalyticsLogger
import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import dev.tuandoan.tasktracker.diagnostics.PerformanceLogger
import io.mockk.mockk

/**
 * Returns a relaxed MockK [BreadcrumbLogger] for tests that construct `TaskManager`
 * (or any other collaborator that depends on breadcrumbs) but don't care to assert on
 * the log calls. Dedicated FB-12 behavior tests pass an explicit mock instead.
 */
fun fakeBreadcrumbLogger(): BreadcrumbLogger = mockk(relaxed = true)

/**
 * Returns a relaxed MockK [AnalyticsLogger] for tests that construct collaborators
 * that depend on Analytics but don't care to assert on the log calls. Dedicated FB-14
 * behavior tests pass an explicit mock instead.
 */
fun fakeAnalyticsLogger(): AnalyticsLogger = mockk(relaxed = true)

/**
 * Returns a relaxed MockK [PerformanceLogger] for tests that construct collaborators
 * that depend on Performance but don't care to assert on the trace calls. Dedicated
 * FB-16/FB-17 behavior tests pass an explicit mock instead.
 */
fun fakePerformanceLogger(): PerformanceLogger = mockk(relaxed = true)
