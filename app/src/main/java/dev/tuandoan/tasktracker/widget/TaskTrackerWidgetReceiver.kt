package dev.tuandoan.tasktracker.widget

import android.content.Context
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
     * considers the broadcast handled.
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
            } finally {
                pendingResult.finish()
            }
        }
    }
}
