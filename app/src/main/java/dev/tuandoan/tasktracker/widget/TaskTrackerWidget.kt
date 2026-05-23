package dev.tuandoan.tasktracker.widget

import android.content.Context
import android.util.Log
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
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
            // Today is the only source wired until V13-09 reads per-appWidgetId
            // configuration (V13-07 / V13-08).
            tasks = WidgetDataProvider(entryPoint.taskDao())
                .getWidgetTasks(source = WidgetSource.Today, limit = WidgetSizeResolver.FETCH_LIMIT)
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
