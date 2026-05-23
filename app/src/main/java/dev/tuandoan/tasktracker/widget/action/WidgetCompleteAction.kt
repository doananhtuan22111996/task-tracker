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
 * Resolves the task manager via [WidgetEntryPoint] and delegates to
 * [WidgetCompleteHandler]. The callback stays thin — all logic (lookup,
 * idempotency, ordering) is in the handler so it's JVM-unit-testable without
 * the Glance runtime.
 *
 * Only calls [TaskTrackerWidget.updateAll] when a completion was actually
 * applied. The handler's no-op paths (missing, already-completed, rapid
 * double-tap absorbed by the DB-state guard) skip the second render — the
 * `WidgetUpdater` injected into `TaskManager` already covers the apply path,
 * so this is a belt-and-braces refresh that picks up other widget instances.
 */
class WidgetCompleteAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val entryPoint = WidgetEntryPoint.get(context)
        val applied = WidgetCompleteHandler(entryPoint.taskManager()).complete(taskId)
        if (applied) {
            TaskTrackerWidget().updateAll(context)
        }
    }

    companion object {
        val TASK_ID_KEY: ActionParameters.Key<Long> = ActionParameters.Key("widget_task_id")
    }
}
