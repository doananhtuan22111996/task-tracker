package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dev.tuandoan.tasktracker.data.preferences.ThemeMode
import dev.tuandoan.tasktracker.widget.model.WidgetSource
import dev.tuandoan.tasktracker.widget.model.WidgetTask
import dev.tuandoan.tasktracker.widget.ui.WidgetContent
import dev.tuandoan.tasktracker.widget.ui.WidgetSizeResolver
import kotlinx.coroutines.flow.first

class TaskTrackerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var tasks: List<WidgetTask> = emptyList()
        var themeMode = ThemeMode.SYSTEM
        var dynamicColor = true

        try {
            val entryPoint = WidgetEntryPoint.get(context)
            // V13-09: resolve the per-appWidgetId source from `widget.preferences_pb`.
            // null = unconfigured (first placement before the user opens the configure
            // activity, OR a transient repository read failure that the repo's IOException
            // recovery already mapped to null) → fall back to the v1.12.0 Today shape so
            // existing placements don't change behavior on upgrade.
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            val source = entryPoint.widgetConfigurationRepository()
                .getSourceOnce(appWidgetId) ?: WidgetSource.Today
            tasks = WidgetDataProvider(entryPoint.taskDao())
                .getWidgetTasks(source = source, limit = WidgetSizeResolver.FETCH_LIMIT)
            val prefs = entryPoint.settingsRepository().userPreferences.first()
            themeMode = prefs.themeMode
            dynamicColor = prefs.dynamicColor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load widget data", e)
        }

        provideContent {
            WidgetTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                WidgetContent(tasks = tasks)
            }
        }
    }

    companion object {
        private const val TAG = "TaskTrackerWidget"

        val SMALL: DpSize = DpSize(110.dp, 110.dp)
        val MEDIUM: DpSize = DpSize(260.dp, 110.dp)
        val LARGE: DpSize = DpSize(260.dp, 260.dp)
    }
}
