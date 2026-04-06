package dev.tuandoan.tasktracker.testutil

import dev.tuandoan.tasktracker.domain.scheduler.WidgetUpdater

class FakeWidgetUpdater : WidgetUpdater {

    var updateCount = 0
        private set

    override suspend fun requestUpdate() {
        updateCount++
    }

    fun reset() {
        updateCount = 0
    }
}
