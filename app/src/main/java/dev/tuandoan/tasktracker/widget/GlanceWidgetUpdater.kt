package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.domain.scheduler.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Glance widget updater that decouples from the calling coroutine scope.
 * Uses its own [CoroutineScope] so updates survive ViewModel scope cancellation.
 * Debounces rapid-fire updates (e.g., bulk operations) with a short delay.
 */
class GlanceWidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) : WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingJob: Job? = null

    override suspend fun requestUpdate() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(DEBOUNCE_MS)
            try {
                TaskTrackerWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget", e)
            }
        }
    }

    companion object {
        private const val TAG = "GlanceWidgetUpdater"
        private const val DEBOUNCE_MS = 300L
    }
}
