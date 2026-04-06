package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.tasktracker.domain.scheduler.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Glance widget updater that uses [NonCancellable] to guarantee the update
 * completes even if the calling coroutine scope is cancelled (e.g., ViewModel
 * scope on navigation). Runs on [Dispatchers.IO] to avoid blocking the main thread.
 */
class GlanceWidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) : WidgetUpdater {

    override suspend fun requestUpdate() {
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                TaskTrackerWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget", e)
            }
        }
    }

    companion object {
        private const val TAG = "GlanceWidgetUpdater"
    }
}
