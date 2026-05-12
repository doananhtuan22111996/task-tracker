package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.diagnostics.BreadcrumbLogger
import io.mockk.mockk

/**
 * Returns a relaxed MockK [BreadcrumbLogger] for tests that construct `TaskManager`
 * (or any other collaborator that depends on breadcrumbs) but don't care to assert on
 * the log calls. Dedicated FB-12 behavior tests pass an explicit mock instead.
 */
fun fakeBreadcrumbLogger(): BreadcrumbLogger = mockk(relaxed = true)
