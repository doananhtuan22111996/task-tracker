package dev.tuandoan.tasktracker.domain.scheduler

/**
 * Interface for triggering widget updates after task mutations.
 */
interface WidgetUpdater {
    suspend fun requestUpdate()
}
