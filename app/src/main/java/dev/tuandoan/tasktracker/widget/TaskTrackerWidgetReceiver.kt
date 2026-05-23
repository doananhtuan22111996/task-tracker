package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.tuandoan.tasktracker.widget.action.WidgetCleanupHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskTrackerWidget()

    /**
     * V13-10: when a widget is removed from the home screen, drop its
     * `widget.preferences_pb` entry so a fresh widget at the same id starts
     * from the default ([WidgetSource.Today]) rather than inheriting the
     * previous user's source choice.
     *
     * `onDeleted` is non-suspend, so we use `goAsync()` — the standard
     * BroadcastReceiver bridge pattern (mirrors [TaskCompleteReceiver]) — to
     * give the suspend `removeConfigs` call time to finish before the system
     * considers the broadcast handled. The launch body is wrapped in
     * `try/catch/finally`: today the handler swallows internally, but if a
     * future refactor moves the swallow elsewhere we don't want an uncaught
     * coroutine exception to escape the fresh `CoroutineScope` and crash
     * ActivityThread.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        val handler = WidgetCleanupHandler(
            WidgetEntryPoint.get(context).widgetConfigurationRepository(),
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.cleanup(appWidgetIds)
            } catch (e: Exception) {
                Log.e(TAG, "Widget config cleanup failed for ids=${appWidgetIds.joinToString()}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TaskTrackerWidgetReceiver"
    }
}
