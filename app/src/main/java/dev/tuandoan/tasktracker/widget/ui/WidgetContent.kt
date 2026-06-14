package dev.tuandoan.tasktracker.widget.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.tuandoan.tasktracker.MainActivity
import dev.tuandoan.tasktracker.R
import dev.tuandoan.tasktracker.widget.action.WidgetCompleteAction
import dev.tuandoan.tasktracker.widget.formatDueDate
import dev.tuandoan.tasktracker.widget.model.WidgetTask

private fun createTaskAction(taskId: Long): Action {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tasktracker://task_editor/$taskId"))
    intent.setClassName("dev.tuandoan.tasktracker", MainActivity::class.java.name)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    return actionStartActivity(intent)
}

private fun createNewTaskAction(): Action {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tasktracker://task_editor"))
    intent.setClassName("dev.tuandoan.tasktracker", MainActivity::class.java.name)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    return actionStartActivity(intent)
}

private fun completeTaskAction(taskId: Long): Action = actionRunCallback<WidgetCompleteAction>(
    parameters = actionParametersOf(WidgetCompleteAction.TASK_ID_KEY to taskId),
)

@Composable
fun WidgetContent(tasks: List<WidgetTask>) {
    val spec = WidgetSizeResolver.resolve(LocalSize.current)
    val visibleTasks = tasks.take(spec.rowCount)

    when (spec.mode) {
        WidgetLayoutMode.COMPACT_BADGE -> WidgetCompactContent(
            tasks = tasks,
            topTask = visibleTasks.firstOrNull(),
        )
        WidgetLayoutMode.LIST -> WidgetListContent(tasks = visibleTasks)
        WidgetLayoutMode.LIST_WITH_OVERDUE -> WidgetListWithOverdueContent(tasks = visibleTasks)
    }
}

@Composable
private fun WidgetListContent(tasks: List<WidgetTask>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(8.dp),
    ) {
        WidgetHeader()
        if (tasks.isEmpty()) {
            WidgetEmptyState()
        } else {
            WidgetTaskList(tasks = tasks)
        }
    }
}

@Composable
private fun WidgetCompactContent(tasks: List<WidgetTask>, topTask: WidgetTask?) {
    val context = LocalContext.current
    val countText = if (tasks.size >= WidgetSizeResolver.FETCH_LIMIT) {
        "${WidgetSizeResolver.FETCH_LIMIT}+"
    } else {
        tasks.size.toString()
    }
    val tapAction = if (topTask != null) createTaskAction(topTask.id) else createNewTaskAction()
    val compactCd = if (topTask != null) {
        context.getString(R.string.cd_widget_compact_summary, tasks.size, topTask.title)
    } else {
        context.getString(R.string.widget_compact_no_tasks)
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(8.dp)
            .semantics { contentDescription = compactCd }
            .clickable(tapAction),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = countText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                ),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = context.getString(R.string.widget_compact_tasks_label),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        if (topTask != null) {
            Text(
                text = topTask.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                ),
                maxLines = 2,
            )
        } else {
            Text(
                text = context.getString(R.string.widget_compact_no_tasks),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun WidgetListWithOverdueContent(tasks: List<WidgetTask>) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val (overdueTasks, regularTasks) = tasks.partition { it.dueAt != null && it.dueAt < now }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(8.dp),
    ) {
        WidgetHeader()
        if (tasks.isEmpty()) {
            WidgetEmptyState()
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                if (overdueTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = context.getString(R.string.widget_overdue_header, overdueTasks.size),
                            style = TextStyle(
                                color = GlanceTheme.colors.error,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            modifier = GlanceModifier.padding(vertical = 2.dp),
                        )
                    }
                    items(overdueTasks, itemId = { it.id }) { task ->
                        WidgetTaskRow(task = task)
                    }
                }
                items(regularTasks, itemId = { it.id }) { task ->
                    WidgetTaskRow(task = task)
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader() {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_name),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .cornerRadius(18.dp)
                .background(GlanceTheme.colors.primary)
                .semantics { contentDescription = context.getString(R.string.widget_add_task) }
                .clickable(createNewTaskAction()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
            )
        }
    }
}

@Composable
private fun WidgetEmptyState() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\uD83D\uDCDD",
                style = TextStyle(fontSize = 24.sp),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_empty_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 14.sp,
                ),
            )
            Text(
                text = context.getString(R.string.widget_empty_subtitle),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun WidgetTaskList(tasks: List<WidgetTask>) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(tasks, itemId = { it.id }) { task ->
            WidgetTaskRow(task = task)
        }
    }
}

@Composable
private fun WidgetTaskRow(task: WidgetTask) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading complete-from-widget checkbox. Real Glance CheckBox renders
        // via RemoteViews so the launcher draws the platform native checkbox
        // with proper accent color + ripple. checked is always false because
        // completed tasks are filtered out of the widget data set; the row
        // disappears on the next provideGlance pass after WidgetCompleteAction
        // runs. No timed strikethrough/fade \u2014 Glance composables compile to a
        // static RemoteViews snapshot and can't host Compose animations
        // (OQ-01 fallback acknowledged in V13-05 decision doc).
        CheckBox(
            checked = false,
            onCheckedChange = completeTaskAction(task.id),
            modifier = GlanceModifier
                .padding(end = 4.dp)
                .semantics {
                    contentDescription = context.getString(R.string.cd_widget_complete_task, task.title)
                },
        )

        // Tap target for the rest of the row \u2192 editor.
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .semantics { contentDescription = context.getString(R.string.cd_widget_open_task, task.title) }
                .clickable(createTaskAction(task.id)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Priority dot
            Text(
                text = when (task.priority) {
                    2 -> "\uD83D\uDD34" // red circle
                    0 -> "\uD83D\uDFE3" // purple circle
                    else -> "\uD83D\uDD35" // blue circle
                },
                style = TextStyle(fontSize = 8.sp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))

            // Pin icon
            if (task.isPinned) {
                Text(
                    text = "\u2605",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
            }

            // Title
            Text(
                text = task.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )

            // Due date
            val dueDateText = formatDueDate(
                dueAt = task.dueAt,
                overdueLabel = context.getString(R.string.widget_due_overdue),
                todayLabel = context.getString(R.string.widget_due_today),
                tomorrowLabel = context.getString(R.string.widget_due_tomorrow),
            )
            if (dueDateText != null) {
                val isOverdue = task.dueAt != null && task.dueAt < System.currentTimeMillis()
                Text(
                    text = dueDateText,
                    style = TextStyle(
                        color = if (isOverdue) {
                            GlanceTheme.colors.error
                        } else {
                            GlanceTheme.colors.onSurfaceVariant
                        },
                        fontWeight = if (isOverdue) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}
