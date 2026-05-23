package dev.tuandoan.tasktracker.widget.action

import android.util.Log
import dev.tuandoan.tasktracker.data.preferences.WidgetConfigurationRepository

/**
 * Pure-Kotlin handler for the widget's `onDeleted` cleanup path. Lifted out of
 * the [TaskTrackerWidgetReceiver] so the bridge logic — "fired with these ids,
 * call removeConfigs with the same ids" — is JVM-unit-testable without the
 * BroadcastReceiver runtime (V13-10).
 *
 * Delegates short-circuit and tag-key cleanup to the repository (V13-07's
 * [WidgetConfigurationRepository.removeConfigs] contract); this handler only
 * owns the failure-swallowing that the receiver needs so a DataStore write
 * error doesn't propagate as a "broadcast handler crashed" to the system.
 */
class WidgetCleanupHandler(private val repository: WidgetConfigurationRepository) {

    suspend fun cleanup(appWidgetIds: IntArray) {
        try {
            repository.removeConfigs(appWidgetIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove widget configs for ids=${appWidgetIds.joinToString()}", e)
        }
    }

    companion object {
        private const val TAG = "WidgetCleanupHandler"
    }
}
