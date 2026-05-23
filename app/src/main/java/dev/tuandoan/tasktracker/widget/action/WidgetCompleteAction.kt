package dev.tuandoan.tasktracker.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import dev.tuandoan.tasktracker.widget.TaskTrackerWidget
import dev.tuandoan.tasktracker.widget.WidgetEntryPoint

/**
 * Glance [ActionCallback] for the per-row complete checkbox.
 *
 * Resolves [ITaskManager] via [WidgetEntryPoint] and delegates to
 * [WidgetCompleteHandler]. The callback itself stays a thin handler — all logic
 * (lookup, idempotency, ordering) is in the handler so it's JVM-unit-testable
 * without the Glance runtime.
 *
 * After completion, every placed widget is refreshed via [TaskTrackerWidget.updateAll]
 * to remove the just-completed row from views fed by other instances. The handler
 * already calls the injected `WidgetUpdater`, so this `updateAll` is the same path
 * — duplicates collapse on the GlanceManager side.
 */
class WidgetCompleteAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val entryPoint = WidgetEntryPoint.get(context)
        WidgetCompleteHandler(entryPoint.taskManager()).complete(taskId)
        TaskTrackerWidget().updateAll(context)
    }

    companion object {
        val TASK_ID_KEY: ActionParameters.Key<Long> = ActionParameters.Key("widget_task_id")
    }
}
